package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.models.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationCodesRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findTopByEmailOrderByCreatedAtDesc(
            String email
    );

    void deleteByEmail(String email);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
