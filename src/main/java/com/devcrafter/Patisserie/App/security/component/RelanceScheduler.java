package com.devcrafter.Patisserie.App.security.component;

import com.devcrafter.Patisserie.App.models.Commande;
import com.devcrafter.Patisserie.App.repository.CommandeRepository;
import com.devcrafter.Patisserie.App.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RelanceScheduler {

    private final CommandeRepository commandeRepository;
    private final EmailService emailService;

    /**
     * Runs every 30 minutes.
     * Finds orders cancelled within the last 24h
     * that haven't received a relance yet,
     * and sends one every 12h.
     */
    @Scheduled(fixedDelay = 30 * 60 * 1000) // every 30 min
    public void envoyerRelances() {

        LocalDateTime now    = LocalDateTime.now();
        LocalDateTime il24h  = now.minusHours(24);

        // Find recently cancelled orders
        List<Commande> cancelled = commandeRepository
                .findCancelledForRelance(il24h, now);

        for (Commande commande : cancelled) {
            LocalDateTime cancelledAt =
                    commande.getUpdatedAt();
            long heuresDepuisAnnulation =
                    java.time.Duration.between(
                            cancelledAt, now).toHours();

            // Send at 12h and 24h after cancellation
            boolean shouldSend = (heuresDepuisAnnulation >= 12
                            && heuresDepuisAnnulation < 13)
                            || (heuresDepuisAnnulation >= 24
                            && heuresDepuisAnnulation < 25);

            if (shouldSend) {
                emailService.sendRelance(commande);
                log.info(
                        "RELANCE sent for order {} " +
                                "({} hours after cancellation)",
                        commande.getNumero(),
                        heuresDepuisAnnulation
                );
            }
        }
    }
}
