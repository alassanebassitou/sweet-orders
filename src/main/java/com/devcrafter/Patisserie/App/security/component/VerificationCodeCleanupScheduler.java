package com.devcrafter.Patisserie.App.security.component;

import com.devcrafter.Patisserie.App.repository.VerificationCodesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeCleanupScheduler {

    private final VerificationCodesRepository codesRepository;

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    @Transactional
    public void cleanupExpiredCodes() {
        codesRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.debug("Expired verification codes cleaned up");
    }
}
