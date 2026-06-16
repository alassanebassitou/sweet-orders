package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.*;
import com.devcrafter.Patisserie.App.dto.request.PaymentRequest;
import com.devcrafter.Patisserie.App.dto.response.PaymentResponse;
import com.devcrafter.Patisserie.App.enums.PaymentMode;
import com.devcrafter.Patisserie.App.enums.PaymentType;
import com.devcrafter.Patisserie.App.helpers.PDFHelperTools;
import com.devcrafter.Patisserie.App.models.Commande;
import com.devcrafter.Patisserie.App.models.Payments;
import com.devcrafter.Patisserie.App.models.SessionUser;
import com.devcrafter.Patisserie.App.repository.CommandeRepository;
import com.devcrafter.Patisserie.App.repository.PaymentsRepository;
import com.devcrafter.Patisserie.App.services.payment.KkiapayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWithKkiapayService {

    private final PaymentsRepository paymentsRepository;
    private final CommandeRepository commandeRepository;
    private final KkiapayService kkiapayService;
    private final PaymentService paymentService;
    private final PDFHelperTools pdfHelperTools;

    // ... your existing methods ...

    /**
     * Initiate Kkiapay payment.
     * Returns a URL to redirect the client to payment page.
     */
    public PaymentResponse verifyAndRecordKkiapay(
            String transactionId,
            Long commandeId,
            SessionUser currentUser,
            String type) {

        // Step 1 — Verify with Kkiapay API
        KkiapayService.KkiapayVerifyResponse verification = kkiapayService.verifyTransaction(transactionId);

        if (!verification.isSuccess()) {
            log.info("Is not success");
            String frontStatus = this.mapKkiapayStatus(verification.getStatus());
            throw new KkiapayPaymentException(verification.getStatus(), frontStatus);
        }

        // Step 2 — Check not already recorded
        Commande commande = commandeRepository
                .findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commande", commandeId
                ));

        boolean alreadyRecorded = paymentsRepository
                .findByCommandeIdOrderByCreatedAtDesc(commandeId)
                .stream()
                .anyMatch(p ->
                        p.getNotes() != null
                                && p.getNotes().contains(transactionId)
                );

        if (alreadyRecorded) {
            throw new ConflictException("Ce paiement a déjà été enregistré");
        }

        // Step 3 — Record payment
        PaymentRequest request = new PaymentRequest();
        request.setCommandeId(commandeId);
        request.setAmount(verification.getAmount());
        request.setPaymentMode(PaymentMode.valueOf(verification.getSource()));
        request.setPaymentDate(LocalDate.now());
        request.setNotes("Kkiapay — transactionId: " + transactionId);
        request.setTransactionId(transactionId);

        if ("ACOMPTE".equals(type)) {
            request.setPaymentType(PaymentType.ACOMPTE);
        } else {
            request.setPaymentType(PaymentType.SOLDE);
            commande.setIsDeliveryFeesPayed(true);
            commandeRepository.save(commande);
        }

        log.info(
                "Kkiapay payment verified and recorded: " +
                        "{} FCFA for order {}",
                verification.getAmount(),
                commande.getNumero()
        );

        return paymentService.recordPayment(request, currentUser);
    }

    @Transactional
    public PaymentResponse verifyAndRecordDeliveryFees(
            String transactionId,
            Long commandeId,
            SessionUser currentUser) {

        // Verify with Kkiapay
        KkiapayService.KkiapayVerifyResponse verify =
                kkiapayService.verifyTransaction(transactionId);

        if (!verify.isSuccess()) {
            throw new BusinessException(
                    "PAYMENT_FAILED",
                    "La vérification du paiement a échoué"
            );
        }

        Commande commande = commandeRepository
                .findById(commandeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Commande", commandeId)
                );

        //Mark delivery fee as paid
        commande.setIsDeliveryFeesPayed(true);
        commandeRepository.save(commande);

        //Record the payment
        Payments payment = new Payments();
        payment.setCommande(commande);
        payment.setAmount(verify.getAmount());
        payment.setPaymentMode(PaymentMode.KKIAPAY);
        payment.setPaymentType(PaymentType.DELIVERY_FEES);
        payment.setPaymentDate(LocalDate.now());
        payment.setNotes("Kkiapay frais livraison — " + transactionId);

        Payments saved = paymentsRepository.save(payment);
        this.pdfHelperTools.generateInvoiceAsync(saved, commande);

        log.info("Delivery fee paid for order {} — {} FCFA",
                commande.getNumero(), verify.getAmount());

        BigDecimal soldeRestant = computeSolde(commande);
        return PaymentResponse.from(saved, soldeRestant);
    }

    /**
     * Process Kkiapay webhook.
     * Called automatically by Kkiapay after payment.
     */
    public void processKkiapayWebhook(String payload, String signature) {

        // Step 1 — Verify signature
        if (!kkiapayService.verifyWebhookSignature(
                payload, signature)) {
            throw new BadRequestException("Invalid webhook signature");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> event = mapper.readValue(payload, Map.class);

            String status = (String) event.get("status");
            String transactionId = (String) event.get("transactionId");
            Integer amount = (Integer) event.get("amount");
            String commandeId = event.get("data").toString();
            String source = (String) event.get("source");

            log.info(
                    "Kkiapay webhook received — " +
                            "status: {} transactionId: {}",
                    status, transactionId
            );

            if (!"SUCCESS".equalsIgnoreCase(status)) {
                log.warn(
                        "Payment not successful: {}",
                        status
                );
                return;
            }

            // Step 2 — Double verify with Kkiapay API
            if (kkiapayService.verifyTransaction(transactionId) == null) {
                log.error(
                        "Transaction {} could not be " +
                                "verified with Kkiapay API",
                        transactionId
                );
                return;
            }

            // Step 3 — Find order by numero
            // transactionId = commande numero (CMD-2026-XXXX)
            Commande commande = commandeRepository
                    .findByNumero(commandeId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                                    "Commande introuvable: " + commandeId
                            )
                    );

            // Step 4 — Check not already recorded
            boolean alreadyPaid = paymentsRepository
                    .findByCommandeIdOrderByCreatedAtDesc(
                            commande.getId())
                    .stream()
                    .anyMatch(p ->
                            p.getNotes() != null
                                    && p.getNotes().contains(transactionId)
                    );

            if (alreadyPaid) {
                log.warn(
                        "Payment {} already recorded",
                        transactionId
                );
                return;
            }

            // Step 5 — Record payment automatically
            Payments payment = new Payments();
            payment.setCommande(commande);
            payment.setAmount(
                    BigDecimal.valueOf(amount)
            );
            payment.setPaymentMode(PaymentMode.valueOf(source));
            payment.setPaymentType(PaymentType.ACOMPTE);
            payment.setPaymentDate(LocalDate.now());
            payment.setNotes("Paiement automatique Kkiapay — " + transactionId);
            paymentsRepository.save(payment);

            log.info(
                    "Payment {} FCFA recorded for " +
                            "order {} via Kkiapay",
                    amount, commande.getNumero()
            );

        } catch (Exception e) {
            log.error(
                    "Webhook processing error: {}",
                    e.getMessage()
            );
            throw new BadRequestException(
                    "Webhook processing failed: "
                            + e.getMessage()
            );
        }
    }

    private String mapKkiapayStatus(String kkiapayStatus) {
        if (kkiapayStatus == null) return "error";
        return switch (kkiapayStatus.toUpperCase()) {
            case "INSUFFICIENT_FUND",
                 "INSUFFICIENT_FUNDS",
                 "LOW_BALANCE"         -> "insufficient_fund";
            case "DECLINED",
                 "TRANSACTION_DENIED",
                 "PAYMENT_DECLINED",
                 "REFUSED",
                 "LIMIT_EXCEEDED"      -> "declined";
            default                    -> "error";
        };
    }

    /**
     * Computes the remaining balance for a commande.
     * Total = products total + delivery fee (if defined)
     * Solde = Total - sum of all payments recorded
     */
    private BigDecimal computeSolde(Commande commande) {

        BigDecimal totalPaye = paymentsRepository
                .totalPaieParCommande(commande.getId());

        BigDecimal grandTotal = commande.getTotalAmount() != null
                ? commande.getTotalAmount()
                : BigDecimal.ZERO;

        // Add delivery fee to grand total only if zone has fee > 0
        if (commande.getDeliveryZone() != null) {
            BigDecimal frais = commande.getDeliveryZone()
                    .getDeliveryFees();
            if (frais != null && frais.compareTo(BigDecimal.ZERO) > 0) {
                grandTotal = grandTotal.add(frais);
            }
        }

        return grandTotal.subtract(totalPaye).max(BigDecimal.ZERO);
    }
}
