package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.models.DeliveryZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, Long> {

    List<DeliveryZone> findAllByActifTrue();

    List<DeliveryZone> findByNameIgnoreCaseAndActifTrue(String name);

    Optional<DeliveryZone> findByNameIgnoreCaseAndNeighborhoodIgnoreCase(
            String name, String quartier
    );

    List<DeliveryZone> findByDeliveryFeesAndActifTrue(
            BigDecimal deliveryFrees
    );

    List<DeliveryZone> findByNameStartingWithIgnoreCaseAndActifTrue(
            String prefix
    );

    List<DeliveryZone> findByNameIgnoreCaseAndNeighborhoodStartingWithIgnoreCaseAndActifTrue(
            String name, String quartierPrefix
    );
}
