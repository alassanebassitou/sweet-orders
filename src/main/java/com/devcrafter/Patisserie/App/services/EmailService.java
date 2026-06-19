package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.models.Commande;
import com.devcrafter.Patisserie.App.models.Invoice;
import com.devcrafter.Patisserie.App.models.User;
import com.devcrafter.Patisserie.App.repository.CommandeRepository;
import com.devcrafter.Patisserie.App.repository.PaymentsRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final PaymentsRepository paymentsRepository;
    private final CommandeRepository commandeRepository;

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
        ctx.setVariable("acompteRequis",
                commande.getRequireAccount());

        send(
                commande.getClient().getEmail(),
                "Commande confirmée — " + commande.getNumero(),
                "emails/order-confirmation",
                ctx
        );
        log.info("ORDER_CONFIRMATION email sent to {}",
                commande.getClient().getEmail());
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
        log.info("ORDER_DELIVERED email sent to {}",
                commande.getClient().getEmail());
    }

    @Async
    @Transactional
    public void sendAcompteReminder(Long commandeId) {

        Commande commande = commandeRepository
                .findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable"));

        if (!hasEmail(commande)) return;

        BigDecimal totalPaye = paymentsRepository
                .totalPaieParCommande(commande.getId());
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
        log.info("ACOMPTE_REMINDER email sent to {}",
                commande.getClient().getEmail());
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
        log.info("RELANCE email sent to {}",
                commande.getClient().getEmail());
    }

    @Async
    public void sendVerificationCode(User user, String code) {
        try {
            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariable("prenom", user.getFirstname());
            ctx.setVariable("code", code);
            ctx.setVariable("appName", appName);

            String html = templateEngine
                    .process("emails/verification-code", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from, fromName);
            helper.setTo(user.getEmail());
            helper.setSubject("🔐 Votre code de connexion — " + appName);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Verification code email sent to {}",
                    user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send verification code: {}",
                    e.getMessage());
            throw new RuntimeException(
                    "Impossible d'envoyer le code. " +
                            "Vérifiez votre email."
            );
        }
    }


    @Async
    public void sendDeliveryFeesIsApplied(Commande commande, BigDecimal frais) {

        if (!hasEmail(commande)) return;

        try {
            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariable("clientPrenom",
                    commande.getClient().getFirstname());
            ctx.setVariable("numeroCommande",
                    commande.getNumero());
            ctx.setVariable("commandeId",
                    commande.getId());
            ctx.setVariable("quartier",
                    commande.getDeliveryZone().getNeighborhood());
            ctx.setVariable("ville",
                    commande.getDeliveryZone().getName());
            ctx.setVariable("fraisLivraison", frais);
            ctx.setVariable("appUrl",  appUrl);
            ctx.setVariable("appName", appName);

            String html = templateEngine.process(
                    "emails/frais-livraison", ctx
            );

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from, fromName);
            helper.setTo(commande.getClient().getEmail());
            helper.setSubject(
                    "🚚 Frais de livraison définis — " +
                            commande.getNumero()
            );
            helper.setText(html, true);

            mailSender.send(message);
            log.info("FRAIS_LIVRAISON_DEFINIS email sent to {}",
                    commande.getClient().getEmail());

        } catch (Exception e) {
            log.error("Failed to send frais email: {}",
                    e.getMessage());
        }
    }

    @Async
    public void sendInvoice(Commande commande,
                            Invoice invoice,
                            byte[] pdfBytes) {
        if (!hasEmail(commande)) return;

        try {
            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariable("clientPrenom",
                    commande.getClient().getFirstname());
            ctx.setVariable("numeroCommande",
                    commande.getNumero());
            ctx.setVariable("numeroFacture", invoice.getNumero());
            ctx.setVariable("montantFacture",
                    invoice.getInvoiceAmount());
            ctx.setVariable("type", invoice.getType().name());
            ctx.setVariable("soldeApres",  invoice.getAfterSold());
            ctx.setVariable("appName", appName);
            ctx.setVariable("appUrl",  appUrl);

            String html = templateEngine.process("emails/invoice", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8");

            helper.setFrom(from, fromName);
            helper.setTo(commande.getClient().getEmail());

            String subject = switch (invoice.getType()) {
                case ACOMPTE ->
                        "🧾 Facture d'acompte — " + commande.getNumero();
                case SOLDE ->
                        "🧾 Facture de solde — " + commande.getNumero();
                case INTEGRAL ->
                        "🧾 Facture — " + commande.getNumero();
                case FRAIS_LIVRAISON ->
                        "🧾 Facture frais de livraison — " +
                                commande.getNumero();
            };
            helper.setSubject(subject);
            helper.setText(html, true);

            // Attach PDF invoice
            helper.addAttachment(
                    invoice.getNumero() + ".pdf",
                    new org.springframework.core.io.ByteArrayResource(pdfBytes),
                    "application/pdf"
            );

            mailSender.send(message);
            log.info("Invoice email sent to {}",
                    commande.getClient().getEmail());

        } catch (Exception e) {
            log.error("Failed to send invoice email: {}",
                    e.getMessage());
        }
    }

    // ─── Private helpers ──────────────────────────────────────

    private Context buildBaseContext(Commande commande) {
        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("clientPrenom",
                commande.getClient().getFirstname());
        ctx.setVariable("clientNom",
                commande.getClient().getLastname());
        ctx.setVariable("numeroCommande",
                commande.getNumero());
        ctx.setVariable("commandeId",
                commande.getId());
        ctx.setVariable("dateLivraison",
                commande.getWishDeliveryDate()
                        .format(DATE_FR));
        ctx.setVariable("montantTotal",
                commande.getTotalAmount());
        ctx.setVariable("appUrl",  appUrl);
        ctx.setVariable("appName", appName);
        return ctx;
    }

    private void send(String to, String subject,
                      String template, Context ctx) {
        try {
            String html = templateEngine
                    .process(template, ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}",
                    to, e.getMessage());
            // Never throw — email failure must not break
            // the main business flow
        }
    }

    private boolean hasEmail(Commande commande) {
        log.info("Client email: {}", commande.getClient().getEmail());
        String email = commande.getClient().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("No email for client {} — skipping",
                    commande.getClient().getId());
            return false;
        }
        return true;
    }

}
