package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.enums.DeliveryStatus;
import com.devcrafter.Patisserie.App.models.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // Today's deliveries
    List<Delivery> findByExpectedDateOrderByExpectedHourAsc(
            LocalDate date
    );

    // By status
    List<Delivery> findByStatutOrderByExpectedDateAsc(
            DeliveryStatus statut
    );

    // Calendar view — month
    List<Delivery> findByExpectedDateBetweenOrderByExpectedDateAsc(
            LocalDate debut,
            LocalDate fin
    );

    // Check if order already has a delivery
    Optional<Delivery> findByCommandeId(Long commandeId);
}
