package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.AccessDeniedException;
import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.dto.request.PaymentRequest;
import com.devcrafter.Patisserie.App.dto.response.PaymentResponse;
import com.devcrafter.Patisserie.App.enums.CommandStatus;
import com.devcrafter.Patisserie.App.enums.PaymentType;
import com.devcrafter.Patisserie.App.helpers.PDFHelperTools;
import com.devcrafter.Patisserie.App.models.Commande;
import com.devcrafter.Patisserie.App.models.Payments;
import com.devcrafter.Patisserie.App.models.SessionUser;
import com.devcrafter.Patisserie.App.repository.CommandeRepository;
import com.devcrafter.Patisserie.App.repository.PaymentsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentsRepository paymentsRepository;
    private final CommandeRepository commandeRepository;
    private final EmailService emailService;
    private final PDFHelperTools pdfHelperTools;

    /**
     * Record a payment for an order.
     * Automatically checks if order is fully paid after recording.
     */
    public PaymentResponse recordPayment(
            PaymentRequest request,
            SessionUser currentUser) {

        Commande commande = commandeRepository
                .findById(request.getCommandeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commande", request.getCommandeId()
                ));

        // Cannot pay a cancelled or already delivered order
        if (commande.getStatus() == CommandStatus.CANCELLED) {
            throw new AccessDeniedException(
                    "Impossible d'enregistrer un paiement " +
                            "sur une commande annulée"
            );
        }

        Payments payments = new Payments();
        payments.setCommande(commande);
        payments.setAmount(request.getAmount());
        payments.setPaymentMode(request.getPaymentMode());
        payments.setPaymentType(request.getPaymentType());

        if (request.getPaymentDate() == null){
            payments.setPaymentDate(LocalDate.now());
        } else {
            payments.setPaymentDate(request.getPaymentDate());
        }
        payments.setNotes(request.getNotes());
        payments.setTransactionId(request.getTransactionId());

        Payments saved = paymentsRepository.save(payments);

        this.pdfHelperTools.generateInvoiceAsync(saved, commande);

        // Calculate remaining balance after this payment
        BigDecimal totalPaye = paymentsRepository
                .totalPaieParCommande(commande.getId());
        BigDecimal soldeRestant = commande.getTotalAmount()
                .subtract(totalPaye)
                .max(BigDecimal.ZERO);

        log.info("Payment of {} FCFA recorded for order {} by {}",
                request.getAmount(),
                commande.getNumero(),
                currentUser.getEmail());

        return PaymentResponse.from(saved, soldeRestant);
    }

    /**
     * List all payments for a specific order.
     * Includes running balance after each payment.
     */
    public List<PaymentResponse> getAllPaymentsByCommande(Long commandeId) {

        commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commande",  commandeId
                ));

        List<Payments> paymentsList = paymentsRepository
                .findByCommandeIdOrderByCreatedAtDesc(commandeId);

        BigDecimal totalPaye = paymentsRepository.totalPaieParCommande(commandeId);

        Commande commande = commandeRepository.findById(commandeId).get();

        BigDecimal soldeRestant = commande.getTotalAmount()
                .subtract(totalPaye)
                .max(BigDecimal.ZERO);

        return paymentsList.stream()
                .map(p -> PaymentResponse.from(p, soldeRestant))
                .toList();
    }

    /**
     * List all payments — admin only.
     * Optional filter by commandeId.
     */
    public List<PaymentResponse> getAllPayments(Long commandeId) {

        if (commandeId != null) {
            return getAllPaymentsByCommande(commandeId);
        }

        return paymentsRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(p -> {
                    BigDecimal totalPaye = paymentsRepository
                            .totalPaieParCommande(p.getCommande().getId());
                    BigDecimal solde = p.getCommande().getTotalAmount()
                            .subtract(totalPaye)
                            .max(BigDecimal.ZERO);
                    return PaymentResponse.from(p, solde);
                })
                .toList();
    }

    /**
     * Cancel a payment — admin only.
     * Recalculates balance after deletion.
     */
    public void cancelledPayment(Long id) {

        Payments payments = paymentsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement", id));

        log.info("Payment {} cancelled for order {}",
                id,
                payments.getCommande().getNumero());

        paymentsRepository.delete(payments);
    }

    /**
     * Get balance summary for an order.
     */
    public Map<String, BigDecimal> getBalanceCommande(
            Long commandeId) {

        Commande commande = commandeRepository
                .findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commande",commandeId
                ));

        BigDecimal totalPaye = paymentsRepository
                .totalPaieParCommande(commandeId);
        BigDecimal soldeRestant = commande.getTotalAmount()
                .subtract(totalPaye)
                .max(BigDecimal.ZERO);

        Map<String, BigDecimal> balance = new HashMap<>();
        balance.put("montantTotal",  commande.getTotalAmount());
        balance.put("acompteRequis", commande.getRequireAccount());
        balance.put("totalPaye",     totalPaye);
        balance.put("soldeRestant",  soldeRestant);
        return balance;
    }

    /**
     * Verified one payment for a specific order.
     */
    @Transactional
    public Boolean verifyIfOrderPayed(Long commandeId) {
        List<Payments> payments = paymentsRepository.findByCommandeId(commandeId);

        return payments.stream()
                .anyMatch(p -> p.getPaymentType() != PaymentType.AVOIR
                && p.getPaymentType() != PaymentType.REMBOURSEMENT);
    }

    /**
     * Admin manually triggers acompte reminder email
     * @param commandeId
     */
    public void sendAcompteReminder(Long commandeId) {
        emailService.sendAcompteReminder(commandeId);
    }
}
