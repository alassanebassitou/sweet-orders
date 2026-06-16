package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.dto.WebSocketEvent;
import com.devcrafter.Patisserie.App.dto.response.NotificationResponse;
import com.devcrafter.Patisserie.App.enums.NotificationTypes;
import com.devcrafter.Patisserie.App.models.Commande;
import com.devcrafter.Patisserie.App.models.Notification;
import com.devcrafter.Patisserie.App.models.User;
import com.devcrafter.Patisserie.App.repository.CommandeRepository;
import com.devcrafter.Patisserie.App.repository.NotificationRepositories;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepositories notificationRepositories;
    private final CommandeRepository commandeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create and broadcast a notification via WebSocket.
     */
    public Notification createAndBroadcast(
            NotificationTypes type,
            String message,
            Commande commande,
            User client) {

        // Save to DB
        Notification notification = new Notification();
        notification.setType(type);
        notification.setMessage(message);
        notification.setCommande(commande);
        notification.setClient(client);
        Notification saved =
                notificationRepositories.save(notification);

        // Broadcast via WebSocket to all connected admins
        WebSocketEvent event = new WebSocketEvent(
                "NOTIFICATION_CREATED",
                NotificationResponse.from(saved)
        );
        messagingTemplate.convertAndSend("/topic/notifications", event);

        log.info("Notification sent: {} — {}",
                type.name(), message);

        return saved;
    }

    /**
     * Broadcast order status change via WebSocket.
     */
    public void diffuserChangementStatut(Commande commande) {
        WebSocketEvent event = new WebSocketEvent(
                "STATUT_CHANGED",
                Map.of(
                        "commandeId",  commande.getId(),
                        "numero",      commande.getNumero(),
                        "statut",      commande.getStatus().name(),
                        "clientEmail", commande.getClient().getEmail()
                )
        );
        messagingTemplate.convertAndSend("/topic/commandes", event);
        log.info("Status change broadcast for order: {}",
                commande.getNumero());
    }

    /**
     * Broadcast new order created via WebSocket.
     */
    public void diffuserNouvelleCommande(Commande commande) {
        WebSocketEvent event = new WebSocketEvent(
                "COMMANDE_CREATED",
                Map.of(
                        "commandeId", commande.getId(),
                        "numero",     commande.getNumero(),
                        "client",     commande.getClient().getEmail(),
                        "montant",    commande.getTotalAmount()
                )
        );
        messagingTemplate.convertAndSend("/topic/commandes", event);
    }

    // ─── REST operations ─────────────────────────────────────────

    public List<NotificationResponse> getToutesNotifications() {
        return notificationRepositories
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public List<NotificationResponse> getUnRead() {
        return notificationRepositories
                .findByIsReadFalseOrderByCreatedAtDesc()
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public long countNonLues() {
        return notificationRepositories.countByIsReadFalse();
    }

    public NotificationResponse markRead(Long id) {
        Notification n = notificationRepositories.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification",id
                ));
        n.setIsRead(true);
        return NotificationResponse.from(
                notificationRepositories.save(n)
        );
    }

    @Transactional
    public void markAllRead() {
        notificationRepositories.markAllAsRead();
    }

    public void remove(Long id) {
        notificationRepositories.deleteById(id);
    }

    // ─── Scheduled checks ────────────────────────────────────────

    /**
     * Every day at 8:00 AM — check tomorrow's deliveries.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void verifierLivraisonsDemain() {
        LocalDate demain = LocalDate.now().plusDays(1);
        List<Commande> commandes = commandeRepository
                .findByWishDeliveryDate(demain);

        for (Commande c : commandes) {
            createAndBroadcast(
                    NotificationTypes.DELIVERY_TOMORROW,
                    "Livraison prévue demain pour "
                            + c.getClient().getFirstname()
                            + " — Commande " + c.getNumero(),
                    c,
                    c.getClient()
            );
        }
        log.info("Checked {} deliveries for tomorrow",
                commandes.size());
    }

    /**
     * Every day at 9:00 AM — check missing deposits (J-2).
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void verifierAcomptesManquants() {
        LocalDate dans2jours = LocalDate.now().plusDays(2);
        List<Commande> commandes = commandeRepository
                .findByWishDeliveryDate(dans2jours);

        for (Commande c : commandes) {
            // Check if acompte has been paid
            BigDecimal totalPaye = notificationRepositories
                    .findAll()
                    .stream()
                    .filter(n -> n.getCommande() != null
                            && n.getCommande().getId()
                            .equals(c.getId()))
                    .count() > 0
                    ? c.getRequireAccount()
                    : BigDecimal.ZERO;

            if (totalPaye.compareTo(BigDecimal.ZERO) > 0) {
                createAndBroadcast(
                        NotificationTypes.MISSING_DEPOSIT,
                        "Acompte non reçu pour la commande "
                                + c.getNumero()
                                + " — Livraison dans 2 jours",
                        c,
                        c.getClient()
                );
            }
        }
    }
}
