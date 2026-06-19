package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.dto.Attachment;
import com.devcrafter.Patisserie.App.dto.Recipient;
import com.devcrafter.Patisserie.App.dto.Sender;
import com.devcrafter.Patisserie.App.dto.request.BrevoEmailRequest;
import com.devcrafter.Patisserie.App.models.Commande;
import com.devcrafter.Patisserie.App.models.Invoice;
import com.devcrafter.Patisserie.App.models.User;
import com.devcrafter.Patisserie.App.repository.CommandeRepository;
import com.devcrafter.Patisserie.App.repository.PaymentsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceWithBrevo {

    private final TemplateEngine templateEngine;
    private final PaymentsRepository paymentsRepository;
    private final CommandeRepository commandeRepository;
    private final RestTemplate restTemplate;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.mail.app-url}")
    private String appUrl;

    @Value("${app.mail.app-name}")
    private String appName;

    @Value("${app.mail.brevo-key}")
    private String brevoApiKey;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final DateTimeFormatter
            DATE_FR = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);

    // ─── Public Async Methods (Completely Intact) ───────────────────────────

    @Async
    public void sendOrderConfirmation(Commande commande) {
        if (!hasEmail(commande)) return;

        Context ctx = buildBaseContext(commande);
        ctx.setVariable("acompteRequis", commande.getRequireAccount());

        send(commande.getClient().getEmail(), "Commande confirmée — " + commande.getNumero(), "emails/order-confirmation", ctx, null);
        log.info("ORDER_CONFIRMATION email sent to {}", commande.getClient().getEmail());
    }

    @Async
    public void sendOrderDelivered(Commande commande) {
        if (!hasEmail(commande)) return;

        Context ctx = buildBaseContext(commande);

        send(commande.getClient().getEmail(), " Votre commande a été livrée !", "emails/order-delivered", ctx, null);
        log.info("ORDER_DELIVERED email sent to {}", commande.getClient().getEmail());
    }

    @Async
    @Transactional
    public void sendAcompteReminder(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable"));

        if (!hasEmail(commande)) return;

        BigDecimal totalPaye = paymentsRepository.totalPaieParCommande(commande.getId());
        BigDecimal soldeRestant = commande.getTotalAmount().subtract(totalPaye).max(BigDecimal.ZERO);

        if (soldeRestant.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("No balance remaining — skipping email");
            return;
        }

        Context ctx = buildBaseContext(commande);
        ctx.setVariable("soldeRestant", soldeRestant);

        send(commande.getClient().getEmail(), " Solde en attente — " + commande.getNumero(), "emails/acompte-reminder", ctx, null);
        log.info("ACOMPTE_REMINDER email sent to {}", commande.getClient().getEmail());
    }

    @Async
    public void sendRelance(Commande commande) {
        if (!hasEmail(commande)) return;

        Context ctx = buildBaseContext(commande);

        send(commande.getClient().getEmail(), "Revenez commander chez nous !", "emails/relance", ctx, null);
        log.info("RELANCE email sent to {}", commande.getClient().getEmail());
    }

    @Async
    public void sendVerificationCode(User user, String code) {
        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("prenom", user.getFirstname());
        ctx.setVariable("code", code);
        ctx.setVariable("appName", appName);

        send(user.getEmail(), "🔐 Votre code de connexion — " + appName, "emails/verification-code", ctx, null);
        log.info("Verification code email sent to {}", user.getEmail());
    }

    @Async
    public void sendDeliveryFeesIsApplied(Commande commande, BigDecimal frais) {
        if (!hasEmail(commande)) return;

        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("clientPrenom", commande.getClient().getFirstname());
        ctx.setVariable("numeroCommande", commande.getNumero());
        ctx.setVariable("commandeId", commande.getId());
        ctx.setVariable("quartier", commande.getDeliveryZone().getNeighborhood());
        ctx.setVariable("ville", commande.getDeliveryZone().getName());
        ctx.setVariable("fraisLivraison", frais);
        ctx.setVariable("appUrl", appUrl);
        ctx.setVariable("appName", appName);

        send(commande.getClient().getEmail(), "🚚 Frais de livraison définis — " + commande.getNumero(), "emails/frais-livraison", ctx, null);
        log.info("FRAIS_LIVRAISON_DEFINIS email sent to {}", commande.getClient().getEmail());
    }

    @Async
    public void sendInvoice(Commande commande, Invoice invoice, byte[] pdfBytes) {
        if (!hasEmail(commande)) return;

        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("clientPrenom", commande.getClient().getFirstname());
        ctx.setVariable("numeroCommande", commande.getNumero());
        ctx.setVariable("numeroFacture", invoice.getNumero());
        ctx.setVariable("montantFacture", invoice.getInvoiceAmount());
        ctx.setVariable("type", invoice.getType().name());
        ctx.setVariable("soldeApres", invoice.getAfterSold());
        ctx.setVariable("appName", appName);
        ctx.setVariable("appUrl", appUrl);

        String subject = switch (invoice.getType()) {
            case ACOMPTE -> "🧾 Facture d'acompte — " + commande.getNumero();
            case SOLDE -> "🧾 Facture de solde — " + commande.getNumero();
            case INTEGRAL -> "🧾 Facture — " + commande.getNumero();
            case FRAIS_LIVRAISON -> "🧾 Facture frais de livraison — " + commande.getNumero();
        };

        // Convert the PDF binary content into a Base64 string for Brevo JSON payload
        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
        Attachment pdfAttachment = new Attachment(invoice.getNumero() + ".pdf", base64Pdf);

        send(commande.getClient().getEmail(), subject, "emails/invoice", ctx, List.of(pdfAttachment));
        log.info("Invoice email sent to {}", commande.getClient().getEmail());
    }

    // ─── Private Unified HTTP Dispatcher ─────────────────────────────────────

    private void send(String to, String subject, String template, Context ctx, List<Attachment> attachments) {
        try {
            String htmlContent = templateEngine.process(template, ctx);

            // 1. Build Headers for API authentication
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            // 2. Map Sender and Recipient profiles
            Sender senderProfile = new Sender(fromName, from);
            List<Recipient> recipientList = List.of(new Recipient(to));

            // 3. Construct Brevo payload structure
            BrevoEmailRequest payload = new BrevoEmailRequest(
                    senderProfile,
                    recipientList,
                    subject,
                    htmlContent,
                    attachments
            );

            HttpEntity<BrevoEmailRequest> request = new HttpEntity<>(payload, headers);

            // 4. Perform secure HTTP Post out to port 443
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, request, String.class);

            if (response.getStatusCode() != HttpStatus.CREATED && response.getStatusCode() != HttpStatus.OK) {
                log.error("Brevo API threw error status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Failed to send email via Brevo REST API to {}: {}", to, e.getMessage());
        }
    }

    private Context buildBaseContext(Commande commande) {
        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("clientPrenom", commande.getClient().getFirstname());
        ctx.setVariable("clientNom", commande.getClient().getLastname());
        ctx.setVariable("numeroCommande", commande.getNumero());
        ctx.setVariable("commandeId", commande.getId());
        ctx.setVariable("dateLivraison", commande.getWishDeliveryDate().format(DATE_FR));
        ctx.setVariable("montantTotal", commande.getTotalAmount());
        ctx.setVariable("appUrl", appUrl);
        ctx.setVariable("appName", appName);
        return ctx;
    }

    private boolean hasEmail(Commande commande) {
        String email = commande.getClient().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("No email for client {} — skipping", commande.getClient().getId());
            return false;
        }
        return true;
    }
}