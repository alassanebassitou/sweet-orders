package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.models.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByCommandeIdOrderByCreatedAtDesc(
            Long commandeId
    );

    Optional<Invoice> findByPaymentId(Long paymentId);

    long countByCommandeId(Long commandeId);
}
