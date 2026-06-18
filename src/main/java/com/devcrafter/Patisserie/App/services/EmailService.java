package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.models.Commande;
import com.devcrafter.Patisserie.App.models.Invoice;
import com.devcrafter.Patisserie.App.models.User;
import com.devcrafter.Patisserie.App.repository.CommandeRepository;
import com.devcrafter.Patisserie.App.repository.PaymentsRepository;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
public class EmailService {

    private final TemplateEngine templateEngine;
    private final PaymentsRepository paymentsRepository;
    private final CommandeRepository commandeRepository;
    private final Resend resend;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.mail.app-url}")
    private String appUrl;

    @Value("${app.mail.app-name}")
    private String appName;

    private static final DateTimeFormatter DATE_FR =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);

    // ─── Public methods ───────────────────────────────────────

    @Async
    public void sendOrderConfirmation(Commande commande) {
        if (!hasEmail(commande)) return;

        Context ctx = buildBaseContext(commande);
        ctx.setVariable("acompteRequis", commande.getRequireAccount());

        send(
                commande.getClient().getEmail(),
                "Commande confirmée — " + commande.getNumero(),
                "emails/order-confirmation",
                ctx
        );
        log.info("ORDER_CONFIRMATION email sent to {}", commande.getClient().getEmail());
    }

    @Async
    public void sendOrderDelivered(Commande commande) {
        if (!hasEmail(commande)) return;

        Context ctx = buildBaseContext(commande);

        send(
                commande.getClient().getEmail(),
                " Votre commande a été livrée !",
                "emails/order-delivered",
                ctx
        );
        log.info("ORDER_DELIVERED email sent to {}", commande.getClient().getEmail());
    }

    @Async
    @Transactional
    public void sendAcompteReminder(Long commandeId) {

        Commande commande = commandeRepository
                .findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable"));

        if (!hasEmail(commande)) return;

        BigDecimal totalPaye = paymentsRepository.totalPaieParCommande(commande.getId());
        BigDecimal soldeRestant = commande.getTotalAmount()
                .subtract(totalPaye)
                .max(BigDecimal.ZERO);

        if (soldeRestant.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("No balance remaining — skipping email");
            return;
        }

        Context ctx = buildBaseContext(commande);
        ctx.setVariable("soldeRestant", soldeRestant);

        send(
                commande.getClient().getEmail(),
                " Solde en attente — " + commande.getNumero(),
                "emails/acompte-reminder",
                ctx
        );
        log.info("ACOMPTE_REMINDER email sent to {}", commande.getClient().getEmail());
    }

    @Async
    public void sendRelance(Commande commande) {
        if (!hasEmail(commande)) return;

        Context ctx = buildBaseContext(commande);

        send(
                commande.getClient().getEmail(),
                "Revenez commander chez nous !",
                "emails/relance",
                ctx
        );
        log.info("RELANCE email sent to {}", commande.getClient().getEmail());
    }

    @Async
    public void sendVerificationCode(User user, String code) {
        try {
            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariable("prenom", user.getFirstname());
            ctx.setVariable("code", code);
            ctx.setVariable("appName", appName);

            String html = templateEngine.process("emails/verification-code", ctx);

            sendViaResend(
                    user.getEmail(),
                    "🔐 Votre code de connexion — " + appName,
                    html,
                    null
            );

            log.info("Verification code email sent to {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send verification code: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer le code. Vérifiez votre email.");
        }
    }

    @Async
    public void sendDeliveryFeesIsApplied(Commande commande, BigDecimal frais) {

        if (!hasEmail(commande)) return;

        try {
            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariable("clientPrenom", commande.getClient().getFirstname());
            ctx.setVariable("numeroCommande", commande.getNumero());
            ctx.setVariable("commandeId", commande.getId());
            ctx.setVariable("quartier", commande.getDeliveryZone().getNeighborhood());
            ctx.setVariable("ville", commande.getDeliveryZone().getName());
            ctx.setVariable("fraisLivraison", frais);
            ctx.setVariable("appUrl", appUrl);
            ctx.setVariable("appName", appName);

            String html = templateEngine.process("emails/frais-livraison", ctx);

            sendViaResend(
                    commande.getClient().getEmail(),
                    "🚚 Frais de livraison définis — " + commande.getNumero(),
                    html,
                    null
            );

            log.info("FRAIS_LIVRAISON_DEFINIS email sent to {}", commande.getClient().getEmail());

        } catch (Exception e) {
            log.error("Failed to send frais email: {}", e.getMessage(), e);
        }
    }

    @Async
    public void sendInvoice(Commande commande, Invoice invoice, byte[] pdfBytes) {
        if (!hasEmail(commande)) return;

        try {
            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariable("clientPrenom", commande.getClient().getFirstname());
            ctx.setVariable("numeroCommande", commande.getNumero());
            ctx.setVariable("numeroFacture", invoice.getNumero());
            ctx.setVariable("montantFacture", invoice.getInvoiceAmount());
            ctx.setVariable("type", invoice.getType().name());
            ctx.setVariable("soldeApres", invoice.getAfterSold());
            ctx.setVariable("appName", appName);
            ctx.setVariable("appUrl", appUrl);

            String html = templateEngine.process("emails/invoice", ctx);

            String subject = switch (invoice.getType()) {
                case ACOMPTE -> "🧾 Facture d'acompte — " + commande.getNumero();
                case SOLDE -> "🧾 Facture de solde — " + commande.getNumero();
                case INTEGRAL -> "🧾 Facture — " + commande.getNumero();
                case FRAIS_LIVRAISON -> "🧾 Facture frais de livraison — " + commande.getNumero();
            };

            Attachment attachment = Attachment.builder()
                    .fileName(invoice.getNumero() + ".pdf")
                    .content(Base64.getEncoder().encodeToString(pdfBytes))
                    .build();

            sendViaResend(
                    commande.getClient().getEmail(),
                    subject,
                    html,
                    List.of(attachment)
            );

            log.info("Invoice email sent to {}", commande.getClient().getEmail());

        } catch (Exception e) {
            log.error("Failed to send invoice email: {}", e.getMessage(), e);
        }
    }

    // ─── Private helpers ──────────────────────────────────────

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

    private void send(String to, String subject, String template, Context ctx) {
        try {
            String html = templateEngine.process(template, ctx);
            sendViaResend(to, subject, html, null);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            // Never throw — email failure must not break the main business flow
        }
    }

    private void sendViaResend(String to, String subject, String html,
                               List<Attachment> attachments) throws ResendException {

        CreateEmailOptions.Builder builder = CreateEmailOptions.builder()
                .from(fromName + " <" + from + ">")
                .to(to)
                .subject(subject)
                .html(html);

        if (attachments != null && !attachments.isEmpty()) {
            builder.attachments(attachments);
        }

        CreateEmailResponse response = resend.emails().send(builder.build());
        log.debug("Resend email id: {}", response.getId());
    }

    private boolean hasEmail(Commande commande) {
        log.info("Client email: {}", commande.getClient().getEmail());
        String email = commande.getClient().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("No email for client {} — skipping", commande.getClient().getId());
            return false;
        }
        return true;
    }
}