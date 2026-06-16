package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.AccessDeniedException;
import com.devcrafter.Patisserie.App.Exceptions.ConflictException;
import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.dto.request.DeliveryRequest;
import com.devcrafter.Patisserie.App.dto.request.FailureRequest;
import com.devcrafter.Patisserie.App.dto.request.ReprogramRequest;
import com.devcrafter.Patisserie.App.dto.response.DeliveryResponse;
import com.devcrafter.Patisserie.App.enums.CommandStatus;
import com.devcrafter.Patisserie.App.enums.DeliveryStatus;
import com.devcrafter.Patisserie.App.models.Commande;
import com.devcrafter.Patisserie.App.models.Delivery;
import com.devcrafter.Patisserie.App.models.SessionUser;
import com.devcrafter.Patisserie.App.repository.CommandeRepository;
import com.devcrafter.Patisserie.App.repository.DeliveryRepository;
import com.devcrafter.Patisserie.App.repository.PaymentsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final CommandeRepository commandeRepository;
    private final PaymentsRepository paiementRepository;

    /**
     * Plan a delivery when order is confirmed.
     * Called automatically when order status → CONFIRMEE.
     */
    public DeliveryResponse planifierLivraison(
            DeliveryRequest request,
            SessionUser currentUser) {

        Commande commande = commandeRepository
                .findById(request.getCommandeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commande" ,request.getCommandeId()
                ));

        // Check order is in a valid state for delivery planning
        if (commande.getStatus() == CommandStatus.CANCELLED
                || commande.getStatus() == CommandStatus.DELIVERED) {
            throw new AccessDeniedException(
                    "Impossible de planifier une delivery pour cette commande"
            );
        }

        // Check delivery doesn't already exist
        deliveryRepository.findByCommandeId(commande.getId())
                .ifPresent(l -> {
                    throw new ConflictException("Une delivery existe déjà pour cette commande");
                });

        Delivery delivery = new Delivery();
        delivery.setCommande(commande);
        delivery.setExpectedDate(request.getExpectedDate());
        delivery.setExpectedHour(request.getExpectedHour());
        delivery.setStatut(DeliveryStatus.PLANNED);
        delivery.setDeliveryAddress(
                commande.getDeliveryAddress()
        );
        delivery.setNotes(request.getNotes());

        Delivery saved = deliveryRepository.save(delivery);
        log.info("Delivery planned for order {} on {}",
                commande.getNumero(), request.getExpectedDate());

        return DeliveryResponse.from(saved,
                getSoldeRestant(commande.getId()));
    }

    /**
     * Get today's deliveries — the daily tour.
     */
    public List<DeliveryResponse> getDailyShot() {
        LocalDate today = LocalDate.now();
        return deliveryRepository
                .findByExpectedDateOrderByExpectedHourAsc(today)
                .stream()
                .filter(l -> l.getStatut() != DeliveryStatus.DELIVERED
                        && l.getStatut() != DeliveryStatus.FAILURE)
                .map(l -> DeliveryResponse.from(l,
                        getSoldeRestant(l.getCommande().getId())))
                .toList();
    }

    /**
     * Get all deliveries with optional status filter.
     */
    public List<DeliveryResponse> getDelivery(
            DeliveryStatus statut) {

        List<Delivery> livraisons = statut != null
                ? deliveryRepository
                .findByStatutOrderByExpectedDateAsc(statut)
                : deliveryRepository.findAll();

        return livraisons.stream()
                .map(l -> DeliveryResponse.from(l,
                        getSoldeRestant(l.getCommande().getId())))
                .toList();
    }

    /**
     * Calendar view — all deliveries for a given month.
     */
    public List<DeliveryResponse> getCalendar(
            int mois, int annee) {

        LocalDate debut = LocalDate.of(annee, mois, 1);
        LocalDate fin   = debut.withDayOfMonth(
                debut.lengthOfMonth()
        );

        return deliveryRepository
                .findByExpectedDateBetweenOrderByExpectedDateAsc(
                        debut, fin)
                .stream()
                .map(l -> DeliveryResponse.from(l,
                        getSoldeRestant(l.getCommande().getId())))
                .toList();
    }

    /**
     * Mark delivery as delivered.
     * Updates order status to LIVREE automatically.
     */
    public DeliveryResponse markDelivered(
            Long id, SessionUser currentUser) {

        Delivery livraison = findOrThrow(id);

        if (livraison.getStatut() == DeliveryStatus.DELIVERED) {
            throw new ConflictException("Cette livraison est déjà marquée comme livrée");
        }

        livraison.setStatut(DeliveryStatus.DELIVERED);
        livraison.setEffectiveDate(LocalDateTime.now());

        // Automatically update order status to DELIVERED
        Commande commande = livraison.getCommande();
        commande.setStatus(CommandStatus.DELIVERED);
        commandeRepository.save(commande);

        Delivery saved = deliveryRepository.save(livraison);

        log.info("Delivery {} marked as delivered by {}",
                id, currentUser.getEmail());

        return DeliveryResponse.from(saved,
                getSoldeRestant(commande.getId()));
    }

    /**
     * Report a delivery failure.
     */
    public DeliveryResponse reportedFailure(
            Long id,
            FailureRequest request,
            SessionUser currentUser) {

        Delivery delivery = findOrThrow(id);

        delivery.setStatut(DeliveryStatus.FAILURE);
        delivery.setFailureReason(request.getFailureReason());
        delivery.setNotes(request.getNotes());

        Delivery saved = deliveryRepository.save(delivery);

        log.info("Delivery {} failed — reason: {} — by {}",
                id, request.getFailureReason(),
                currentUser.getEmail());

        return DeliveryResponse.from(saved,
                getSoldeRestant(
                        delivery.getCommande().getId()));
    }

    /**
     * Reschedule a failed delivery.
     */
    public DeliveryResponse reprogramme(
            Long id,
            ReprogramRequest request,
            SessionUser currentUser) {

        Delivery delivery = findOrThrow(id);

        if (delivery.getStatut() == DeliveryStatus.DELIVERED) {
            throw new ConflictException(
                    "Impossible de reprogrammer une delivery déjà livrée"
            );
        }

        delivery.setExpectedDate(request.getNewDate());
        delivery.setExpectedHour(request.getNewHour());
        delivery.setStatut(DeliveryStatus.REPROGRAMMED);
        delivery.setNotes(request.getNotes());
        delivery.setFailureReason(null);

        Delivery saved = deliveryRepository.save(delivery);

        log.info("Delivery {} rescheduled to {} by {}",
                id, request.getNewDate(),
                currentUser.getEmail());

        return DeliveryResponse.from(saved,
                getSoldeRestant(
                        delivery.getCommande().getId()));
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private BigDecimal getSoldeRestant(Long commandeId) {
        Commande commande = commandeRepository
                .findById(commandeId).orElse(null);
        if (commande == null) return BigDecimal.ZERO;

        BigDecimal totalPaye = paiementRepository
                .totalPaieParCommande(commandeId);
        return commande.getTotalAmount()
                .subtract(totalPaye)
                .max(BigDecimal.ZERO);
    }

    private Delivery findOrThrow(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Livraison", id
                ));
    }
}
