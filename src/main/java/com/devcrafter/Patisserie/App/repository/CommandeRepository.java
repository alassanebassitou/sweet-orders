package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.enums.CommandStatus;
import com.devcrafter.Patisserie.App.models.Commande;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommandeRepository extends JpaRepository<Commande, Long> {

    // Client my orders
    List<Commande> findByClientIdOrderByCreatedAtDesc(Long clientId);

    // Admin filter by status
    List<Commande> findByStatusOrderByCreatedAtDesc(
            CommandStatus statut
    );

    // Admin all orders
    List<Commande> findAllByOrderByCreatedAtDesc();

    boolean existsByNumero(String numero);

    List<Commande> findByWishDeliveryDate(LocalDate date);

    Optional<Commande> findByNumero(String transactionId);

    // In CommandeRepository
    @Query("SELECT COUNT(c) FROM Commande c " +
            "WHERE c.client.id = :clientId")
    Integer countByClientId(@Param("clientId") Long clientId);

    @Query("SELECT c FROM Commande c " +
            "WHERE c.status = 'CANCELLED' " +
            "AND c.updatedAt BETWEEN :debut AND :fin " +
            "AND c.client.email IS NOT NULL")
    List<Commande> findCancelledForRelance(
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin
    );

    @Query("SELECT c FROM Commande c " +
            "WHERE c.deliveryMode = 'HOME_DELIVERY' " +
            "AND c.isDeliveryFeesApplied = true " +
            "AND c.status NOT IN ('CANCELLED', 'DELIVERED')")
    List<Commande> findCommandesAvecFraisNonDefinis();

    // Orders by payment status of delivery fee
    @Query("SELECT c FROM Commande c " +
            "LEFT JOIN c.deliveryZone z " +
            "WHERE z.deliveryFees > 0 " +
            "AND c.isDeliveryFeesPayed = false " +
            "AND c.status NOT IN ('CANCELLED')")
    List<Commande> findCommandesAvecFraisImpaye();

    // In CommandeRepository
    @Query("SELECT c FROM Commande c " +
            "LEFT JOIN FETCH c.orderedProducts op " +
            "LEFT JOIN FETCH op.products " +
            "LEFT JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.deliveryZone " +
            "WHERE c.id = :id")
    Optional<Commande> findByIdWithDetails(@Param("id") Long id);
}
