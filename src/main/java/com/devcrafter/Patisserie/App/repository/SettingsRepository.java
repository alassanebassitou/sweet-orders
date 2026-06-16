package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.models.Settings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettingsRepository extends JpaRepository<Settings, Long> {

    Optional<Settings> findByCle(String cle);
    List<Settings> findAll();
}
