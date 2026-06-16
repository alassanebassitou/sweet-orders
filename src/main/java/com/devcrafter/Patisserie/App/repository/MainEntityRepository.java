package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MainEntityRepository <T extends User> extends JpaRepository<T, Long> {
}
