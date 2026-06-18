package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.AccessDeniedException;
import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.Exceptions.BusinessException;
import com.devcrafter.Patisserie.App.dto.request.*;
import com.devcrafter.Patisserie.App.dto.response.CommandeBalanceResponse;
import com.devcrafter.Patisserie.App.dto.response.CommandeResponse;
import com.devcrafter.Patisserie.App.enums.*;
import com.devcrafter.Patisserie.App.models.*;
import com.devcrafter.Patisserie.App.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final CommandeHistoricStatusRepository historicStatusRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PaymentsRepository paymentsRepository;
    private final DeliveryRepository deliveryRepository;
    private final EmailService emailService;
    private final DeliveryZoneRepository zoneRepository;
    private final SettingsRepository settingsRepository;
    private final ProductCustomerRepository customerRepository;

    /**
     * Client creates a new order from the app.
     */
    public CommandeResponse createdCommande(
            CommandeRequest request,
            SessionUser currentUser) {

        User client = userRepository
                .findById(currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found"
                ));

        Commande commande = new Commande();
        commande.setNumero(genererNumero());
        commande.setClient(client);
        commande.setStatus(CommandStatus.PENDING_CONFIRMATION);
        commande.setWishDeliveryDate(request.getWishDeliveryDate());
        commande.setCreneauHoraire(request.getCreneauHoraire());
        commande.setDeliveryMode(
                request.getDeliveryMode() != null
                        ? request.getDeliveryMode()
                        : DeliveryMode.HOME_DELIVERY
        );
        commande.setDeliveryAddress(request.getDeliveryAddress());
        commande.setDeliveryInstruction(request.getDeliveryInstruction());
        commande.setIsEmergency(
                request.getIsEmergency() != null
                        ? request.getIsEmergency()
                        : false
        );
        commande.setCommandDate(LocalDateTime.now());

        if (request.getSource() != null) {
            commande.setSource(OrderSource.valueOf(request.getSource()));
        } else {
            commande.setSource(OrderSource.APP);
        }

        log.info("Delivery Fees Is Applied: {}", request.getIsDeliveryFeesApplied());

        // ─── Delivery zone ────────────────────────────────────
        commande.setIsDeliveryFeesPayed(false);
        commande.setIsDeliveryFeesApplied(false);

        if (request.getDeliveryMode() == DeliveryMode.HOME_DELIVERY
                && request.getCity() != null && request.getNeighborhood() != null) {

            Optional<DeliveryZone> zone = zoneRepository
                    .findByNameIgnoreCaseAndNeighborhoodIgnoreCase(
                            request.getCity(),
                            request.getNeighborhood()
                    );

            if (zone.isPresent()) {
                commande.setDeliveryZone(zone.get());
                commande.setIsDeliveryFeesApplied(
                        zone.get().getDeliveryFees()
                                .compareTo(BigDecimal.ZERO) == 0
                );

            } else {
                // Unknown quartier — zone not in DB yet
                commande.setIsDeliveryFeesApplied(true);
                log.info("Unknown quartier: {} / {} — marked",
                        request.getCity(), request.getNeighborhood());
            }
        }

        // ─── Products + total ─────────────────────────────────
        BigDecimal total = BigDecimal.ZERO;

        for (OrderedProductRequest cp : request.getProductRequests()) {
            Products product = productRepository
                    .findById(cp.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produit", cp.getProductId()
                    ));

            BigDecimal unitPrice  = product.getBasePrice();

            if (cp.getSelectedCustomizationIds() != null
                    && !cp.getSelectedCustomizationIds().isEmpty()) {

                BigDecimal extrasTotal = cp.getSelectedCustomizationIds()
                        .stream()
                        .map(cid -> customerRepository
                                .findById(cid)
                                .map(ProductCustomization::getAdditionalPrice)
                                .orElse(BigDecimal.ZERO)
                        )
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                unitPrice = unitPrice.add(extrasTotal);
            }
            int qty =  cp.getQuantity() != null
                    ? cp.getQuantity() : 1;
            BigDecimal lineTotal  = unitPrice
                    .multiply(BigDecimal.valueOf(qty));
            total = total.add(lineTotal);

            OrderedProducts ligne = new OrderedProducts();
            ligne.setCommande(commande);
            ligne.setProducts(product);
            ligne.setQuantity(qty);
            ligne.setCustomizationJson(
                    cp.getCustomizationJson() != null
                            ? cp.getCustomizationJson()
                            : new HashMap<>()
            );
            ligne.setCakeMessage(cp.getCakeMessage());
            ligne.setAllergen(cp.getAllergen());
            ligne.setUnitPrice(unitPrice);
            ligne.setTotalPrice(lineTotal);
            commande.getOrderedProducts().add(ligne);
        }

        // ─── VIP discount ─────────────────────────────────────
        if (client instanceof Client vipClient
                && Boolean.TRUE.equals(vipClient.getIsVIP())
                && vipClient.getRemiseVIP() != null
                && vipClient.getRemiseVIP()
                .compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal discountRate = vipClient.getRemiseVIP()
                    .divide(BigDecimal.valueOf(100));
            BigDecimal discount = total.multiply(discountRate);
            total = total.subtract(discount);

            log.info("VIP discount {}% applied — saved {} FCFA",
                    vipClient.getRemiseVIP(), discount);
        }

        // Set products total (without delivery fee)
        commande.setTotalAmount(total);

        // ─── Acompte calculation ──────────────────────────────
        BigDecimal frais = BigDecimal.ZERO;
        if (commande.getDeliveryZone() != null
                && commande.getDeliveryZone().getDeliveryFees() != null) {
            frais = commande.getDeliveryZone().getDeliveryFees();
        }

        BigDecimal totalAvecFrais = total.add(frais);
        BigDecimal acompte = totalAvecFrais
                .multiply(getAcomptePercent())
                .setScale(0, RoundingMode.CEILING);
        commande.setRequireAccount(acompte);

        // ─── Historic ─────────────────────────────────────────
        ajouterHistorique(
                commande, null,
                CommandStatus.PENDING_CONFIRMATION,
                "Commande créée via l'app",
                currentUser.getEmail()
        );

        // ─── Save ─────────────────────────────────────────────
        Commande saved = commandeRepository.save(commande);
        log.info("Order {} created by {}",
                saved.getIsDeliveryFeesApplied(), currentUser.getEmail());

        // ─── Notifications ────────────────────────────────────
        notificationService.diffuserNouvelleCommande(saved);
        notificationService.createAndBroadcast(
                NotificationTypes.NEW_ORDER,
                "Nouvelle commande " + saved.getNumero()
                        + " de " + saved.getClient().getFirstname(),
                saved,
                saved.getClient()
        );

        // Fix: guard before accessing zoneLivraison
        if (Boolean.TRUE.equals(saved.getIsDeliveryFeesApplied())) {
            String zoneInfo = saved.getDeliveryZone() != null
                    ? saved.getDeliveryZone().getName()
                    + " / "
                    + saved.getDeliveryZone().getNeighborhood()
                    : "Ville / Quartier";

            notificationService.createAndBroadcast(
                    NotificationTypes.DELIVERY_FEES_NOT_APPLIED,
                    "Quartier non répertorié : "
                            + zoneInfo
                            + " — Commande " + saved.getNumero(),
                    saved,
                    null
            );
            log.info("Admin notified of unknown quartier: {}",
                    zoneInfo);
        }

        return buildResponse(saved);
    }

    @Transactional
    public CommandeResponse applyDeliveryFee(
            Long commandeId,
            ApplyDeliveryFeeRequest request,
            SessionUser admin) {

        Commande commande = findOrThrow(commandeId);

        BigDecimal frais = request.getDeliveryFees();

        // Update commande
        //commande.setFraisLivraison(frais);
        commande.setIsDeliveryFeesApplied(false);

        // Update the zone if zoneId provided
        if (request.getZoneId() != null) {
            zoneRepository.findById(request.getZoneId())
                    .ifPresent(zone -> {
                        commande.setDeliveryZone(zone);
                        // Also update zone fee if it was 0
                        if (zone.getDeliveryFees()
                                .compareTo(BigDecimal.ZERO) == 0) {
                            zone.setDeliveryFees(frais);
                            zoneRepository.save(zone);
                            log.info("Zone {} / {} fee updated to {} FCFA",
                                    zone.getName(), zone.getNeighborhood(), frais);
                        }
                    });
        } else {
            // Try to find and update zone by ville + quartier
            if (commande.getDeliveryZone() != null ) {
                zoneRepository.findByNameIgnoreCaseAndNeighborhoodIgnoreCase(
                                commande.getDeliveryZone().getName(),
                                commande.getDeliveryZone().getNeighborhood()
                        )
                        .ifPresent(zone -> {
                            commande.setDeliveryZone(zone);
                            if (zone.getDeliveryFees()
                                    .compareTo(BigDecimal.ZERO) == 0) {
                                zone.setDeliveryFees(frais);
                                zoneRepository.save(zone);
                            }
                        });
            }
        }

        // Recalculate acompte with new total
        BigDecimal totalAvecFrais = commande.getTotalAmount().add(frais);
        BigDecimal acompte = totalAvecFrais.multiply(
                getAcomptePercent()
        ).setScale(0, RoundingMode.CEILING);
        commande.setRequireAccount(acompte);

        Commande saved = commandeRepository.save(commande);

        log.info("Delivery fee {} FCFA applied to order {} by admin {}",
                frais, saved.getNumero(), admin.getEmail());

        // Notify client via WebSocket
        notificationService.createAndBroadcast(
                NotificationTypes.DELIVERY_FEES_APPLIED,
                "Vos frais de livraison ont été définis : " +
                        frais + " FCFA — Commande " + saved.getNumero(),
                saved,
                saved.getClient()
        );

        // Send email to client
        emailService.sendDeliveryFeesIsApplied(saved, frais);

        return buildResponse(saved);
    }

    /**
     * Client views their own orders.
     */
    public List<CommandeResponse> getMyCommandes(SessionUser currentUser) {
        return commandeRepository.findByClientIdOrderByCreatedAtDesc(
                        currentUser.getUserId())
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    /**
     * Get one order — client can only see their own.
     */
    public CommandeResponse getCommande(Long id, SessionUser currentUser) {
        Commande commande = findOrThrow(id);

        // Client can only see their own orders
        boolean isAdmin = currentUser.getRole()
                .equals("ROLE_ADMIN");

        boolean isOwner = commande.getClient().getId()
                .equals(currentUser.getUserId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "Accès refusé à cette commande"
            );
        }
        return this.buildResponse(commande);
    }

    /**
     * Client duplicates a previous order.
     */
    public CommandeResponse dupliquerCommande(Long id, LocalDate date,
                                              SessionUser currentUser) {
        Commande original = findOrThrow(id);

        // Verify ownership
        if (!original.getClient().getId()
                .equals(currentUser.getUserId())) {
            throw new AccessDeniedException("Accès refusé");
        }

        Commande copie = new Commande();
        copie.setNumero(genererNumero());
        copie.setClient(original.getClient());
        copie.setStatus(CommandStatus.DRAFT);
        copie.setDeliveryMode(original.getDeliveryMode());
        copie.setDeliveryAddress(original.getDeliveryAddress());
        copie.setSource(OrderSource.APP);
        copie.setWishDeliveryDate(date);
        copie.setCommandDate(LocalDateTime.now());

        // Copy product lines
        for (OrderedProducts cp : original.getOrderedProducts()) {
            OrderedProducts ligne = new OrderedProducts();
            ligne.setCommande(copie);
            ligne.setProducts(cp.getProducts());
            ligne.setQuantity(cp.getQuantity());
            ligne.setCustomizationJson(
                    cp.getCustomizationJson());
            ligne.setCakeMessage(cp.getCakeMessage());
            ligne.setAllergen(cp.getAllergen());
            ligne.setUnitPrice(cp.getUnitPrice());
            ligne.setTotalPrice(cp.getTotalPrice());
            copie.getOrderedProducts().add(ligne);
        }

        copie.setTotalAmount(original.getTotalAmount());
        copie.setRequireAccount(original.getRequireAccount());

        ajouterHistorique(copie, null,
                CommandStatus.DRAFT,
                "Dupliquée depuis commande " + original.getNumero(),
                currentUser.getEmail());

        return this.buildResponse(commandeRepository.save(copie));
    }

    /**
     * Delete all commandes that id is into ids
     * @param request
     */
    @Transactional
    public void deleteCommandes(DeleteCommandeRequest request) {
        commandeRepository.deleteAllById(request.getIds());
    }

    @Transactional
    public void deleteOneCommande(Long id) {
        Commande commande = this.findOrThrow(id);
        commandeRepository.delete(commande);
    }

    // ─── Admin endpoints ──────────────────────────────────────────

    /**
     * Admin views all orders — with optional status filter.
     */
    public List<CommandeResponse> getAllCommandes(CommandStatus status) {

        List<Commande> commandes = status != null
                ? commandeRepository
                .findByStatusOrderByCreatedAtDesc(status)
                : commandeRepository.findAllByOrderByCreatedAtDesc();

        return commandes.stream()
                .map(this::buildResponse)
                .toList();
    }

    /**
     * Admin changes order status.
     * Enforces valid transitions.
     */
    public CommandeResponse changerStatut(Long id,
                                          StatusRequest request,
                                          SessionUser currentUser) {
        Commande commande = findOrThrow(id);
        CommandStatus old  = commande.getStatus();
        CommandStatus newStat = request.getStatus();

        if (newStat == CommandStatus.DELIVERED) {
            BigDecimal totalPaye = paymentsRepository
                    .totalPaieParCommande(commande.getId());
            BigDecimal grandTotal = commande.getTotalAmount();

            // Add delivery fee if applicable
            if (commande.getDeliveryZone() != null
                    && commande.getDeliveryZone()
                    .getDeliveryFees() != null
                    && Boolean.TRUE.equals(
                    commande.getIsDeliveryFeesPayed())) {
                grandTotal = grandTotal.add(
                        commande.getDeliveryZone().getDeliveryFees()
                );
            }

            BigDecimal soldeRestant = grandTotal
                    .subtract(totalPaye)
                    .max(BigDecimal.ZERO);

            if (soldeRestant.compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessException(
                        "PAYMENT_REQUIRED",
                        "Cette commande ne peut pas être marquée " +
                                "livrée car un solde de " +
                                soldeRestant + " FCFA reste impayé. " +
                                "Veuillez enregistrer le paiement complet " +
                                "avant de marquer la commande comme livrée."
                );
            }
        }

        // Block any status change if already DELIVERED
        if (old == CommandStatus.DELIVERED) {
            throw new BusinessException(
                    "ORDER_ALREADY_DELIVERED",
                    "Cette commande a déjà été livrée. " +
                            "Aucun changement de statut n'est possible."
            );
        }

        validerTransition(old, newStat);
        commande.setStatus(newStat);
        ajouterHistorique(commande, old, newStat,
                request.getCommentaire(), currentUser.getEmail());
        Commande saved = commandeRepository.save(commande);

        switch (newStat) {
            case CONFIRMED ->
                    emailService.sendOrderConfirmation(saved);

            case DELIVERED ->
                    emailService.sendOrderDelivered(saved);

            default -> {}
        }

        if (newStat == CommandStatus.CONFIRMED) {
            createDeliveryAutomatically(saved);
        }

        log.info("Order {} status: {} → {} by {}",
                commande.getNumero(), old, newStat,
                currentUser.getEmail());

        notificationService.diffuserChangementStatut(commande);

        if (newStat == CommandStatus.READY) {
            notificationService.createAndBroadcast(
                    NotificationTypes.ORDER_READY,
                    "La commande " + commande.getNumero()
                            + " est prête pour livraison !",
                    commande,
                    commande.getClient()
            );
        }

        return this.buildResponse(saved);
    }

    public CommandeBalanceResponse balance(Long commandId) {
        Commande commande = this.findOrThrow(commandId);

        BigDecimal totalPaye = paymentsRepository
                .totalPaieParCommande(commandId);

        BigDecimal soldeRestant = commande.getTotalAmount()
                .subtract(totalPaye)
                .max(BigDecimal.ZERO);

        CommandeBalanceResponse r = new CommandeBalanceResponse();
        r.setTotalAmount(commande.getTotalAmount());
        r.setRequireAccount(commande.getRequireAccount());
        r.setNetProfit(soldeRestant);
        r.setTotalPaye(totalPaye);

        return r;
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private CommandeResponse buildResponse(Commande c) {

        BigDecimal totalPaye = paymentsRepository
                .totalPaieParCommande(c.getId());

        BigDecimal soldeRestant = c.getTotalAmount()
                .subtract(totalPaye)
                .max(BigDecimal.ZERO);

        BigDecimal solde = c.getTotalAmount()
                .subtract(totalPaye)
                .max(BigDecimal.ZERO);

        CommandeResponse r = CommandeResponse.from(c);
        r.setTotalPaye(totalPaye);
        r.setNetProfit(soldeRestant);
        r.setIsFullyPaid(solde.compareTo(BigDecimal.ZERO) == 0);
        return r;
    }


    private void validerTransition(CommandStatus old, CommandStatus newStat) {
        // ANNULEE is always allowed from any status
        if (newStat == CommandStatus.CANCELLED) return;

        // Define valid transitions
        Map<CommandStatus, List<CommandStatus>> transitions = Map.of(
                CommandStatus.DRAFT,
                List.of(CommandStatus.PENDING_CONFIRMATION,
                        CommandStatus.CANCELLED),
                CommandStatus.PENDING_CONFIRMATION,
                List.of(CommandStatus.CONFIRMED,
                        CommandStatus.CANCELLED),
                CommandStatus.CONFIRMED,
                List.of(CommandStatus.IN_PRODUCTION,
                        CommandStatus.CANCELLED),
                CommandStatus.IN_PRODUCTION,
                List.of(CommandStatus.READY,
                        CommandStatus.CANCELLED),
                CommandStatus.READY,
                List.of(CommandStatus.DELIVERED,
                        CommandStatus.CANCELLED),
                CommandStatus.DELIVERED,
                List.of(),
                CommandStatus.CANCELLED,
                List.of()
        );

        List<CommandStatus> allowed = transitions
                .getOrDefault(old, List.of());

        if (!allowed.contains(newStat)) {
            throw new BusinessException("INVALID_TRANSACTION",
                    "Transition de statut invalide: " + old + " → " + newStat
            );
        }
    }

    private void ajouterHistorique(Commande commande,
                                   CommandStatus ancien,
                                   CommandStatus nouveau,
                                   String commentaire,
                                   String email) {
        CommandeHistoricStatus h = new CommandeHistoricStatus();
        h.setCommande(commande);
        h.setOldStatus(ancien);
        h.setNewStatus(nouveau);
        h.setCommentaire(commentaire);
        commande.getHistoricStatuses().add(h);
    }

    private String genererNumero() {
        String year = String.valueOf(
                LocalDate.now().getYear());
        String uuid = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
        return "CMD-" + year + "-" + uuid;
    }

    private Commande findOrThrow(Long id) {
        return commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commande", id
                ));
    }

    private void createDeliveryAutomatically(Commande commande) {

        boolean exists = deliveryRepository
                .findByCommandeId(commande.getId())
                .isPresent();

        if (exists) {
            log.info("Delivery already exists for order {}", commande.getNumero());
            return;
        }

        Delivery delivery = new Delivery();
        delivery.setCommande(commande);
        delivery.setStatut(DeliveryStatus.PLANNED);
        delivery.setExpectedDate(commande.getWishDeliveryDate());
        delivery.setExpectedHour(commande.getCreneauHoraire());
        delivery.setDeliveryAddress(commande.getDeliveryAddress());

        if (commande.getDeliveryMode() == DeliveryMode.COLLECTION_ON_SITE) {
            delivery.setNotes("Retrait sur place");
        }

        deliveryRepository.save(delivery);

        log.info(
                "Delivery auto-created for order {} — date: {}",
                commande.getNumero(),
                commande.getWishDeliveryDate()
        );
    }

    private BigDecimal getAcomptePercent() {
        String pct = settingsRepository.findByCle("pourcentage_acompte")
                .map(Settings::getValue)
                .orElse("50");
        return new BigDecimal(pct).divide(BigDecimal.valueOf(100));
    }
}
