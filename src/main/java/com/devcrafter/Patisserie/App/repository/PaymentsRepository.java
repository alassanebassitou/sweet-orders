package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.models.Payments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentsRepository extends JpaRepository<Payments, Long> {

    List<Payments> findByCommandeIdOrderByCreatedAtDesc(
            Long commandeId
    );

    //List<Payments> findByTransactionIdOrderByCreatedAtDesc(String transactionId);

    List<Payments> findAllByOrderByCreatedAtDesc();

    // Total paid for a specific order
    @Query("SELECT COALESCE(SUM(p.amount), 0) " +
            "FROM Payments p " +
            "WHERE p.commande.id = :commandeId " +
            "AND p.paymentType NOT IN " +
            "('AVOIR', 'REMBOURSEMENT')")
    BigDecimal totalPaieParCommande(@Param("commandeId") Long commandeId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) " +
            "FROM Payments p " +
            "WHERE p.commande.client.id = :clientId " +
            "AND p.paymentType NOT IN ('AVOIR', 'REMBOURSEMENT')")
    BigDecimal totalExpenseByClient(@Param("clientId") Long clientId);

    List<Payments> findByCommandeId(Long commandeId);
}
