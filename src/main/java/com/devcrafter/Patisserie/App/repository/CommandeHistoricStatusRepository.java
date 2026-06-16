package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.models.CommandeHistoricStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommandeHistoricStatusRepository extends JpaRepository<CommandeHistoricStatus, Long> {

    List<CommandeHistoricStatus> findByCommandeIdOrderByUpdatedAtDesc(
            Long commandeId
    );
}
