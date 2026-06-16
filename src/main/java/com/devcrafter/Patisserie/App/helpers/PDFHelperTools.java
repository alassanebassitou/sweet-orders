package com.devcrafter.Patisserie.App.helpers;

import com.devcrafter.Patisserie.App.models.Commande;
import com.devcrafter.Patisserie.App.models.Payments;
import com.devcrafter.Patisserie.App.repository.PaymentsRepository;
import com.devcrafter.Patisserie.App.services.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PDFHelperTools {

    private final InvoiceService invoiceService;
    private final PaymentsRepository paymentsRepository;

    public void generateInvoiceAsync(Payments payment, Commande commande) {
        // Get total paid BEFORE this payment
        BigDecimal paidBefore = paymentsRepository
                .totalPaieParCommande(commande.getId())
                .subtract(payment.getAmount())
                .max(BigDecimal.ZERO);

        // Generate in background — never blocks payment flow
        invoiceService.generateAndSendAsync(payment, commande.getId(), paidBefore);
    }
}
