package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.Delivery;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class DeliveryResponse {

    private Long id;
    private Long commandeId;
    private String noCommande;
    private String clientName;
    private String clientPhone;
    private String deliveryAddress;
    private String status;
    private LocalDate expectedDate;
    private String expectedHour;
    private LocalDateTime effectiveDate;
    private String failureReason;
    private String notes;
    private BigDecimal restSolde;

    // Order summary
    private List<String> products;
    private LocalDateTime createdAt;

    public static DeliveryResponse from(Delivery l,
                                        BigDecimal soldeRestant) {
        DeliveryResponse r = new DeliveryResponse();
        r.setId(l.getId());
        r.setCommandeId(l.getCommande().getId());
        r.setNoCommande(l.getCommande().getNumero());
        r.setClientName(
                l.getCommande().getClient().getLastname()
                        + " " +
                        l.getCommande().getClient().getFirstname()
        );
        r.setClientPhone(
                l.getCommande().getClient().getTelephone()
        );
        r.setDeliveryAddress(l.getDeliveryAddress() != null
                ? l.getDeliveryAddress()
                : l.getCommande().getDeliveryAddress()
        );
        r.setStatus(l.getStatut().name());
        r.setExpectedDate(l.getExpectedDate());
        r.setExpectedHour(l.getExpectedHour());
        r.setEffectiveDate(l.getEffectiveDate());
        r.setFailureReason(l.getFailureReason() != null
                ? l.getFailureReason().name() : null
        );
        r.setNotes(l.getNotes());
        r.setRestSolde(soldeRestant);
        r.setProducts(
                l.getCommande().getOrderedProducts().stream()
                        .map(cp -> cp.getQuantity() + "x "
                                + cp.getProducts().getName())
                        .collect(Collectors.toList())
        );
        r.setCreatedAt(l.getCreatedAt());
        return r;
    }
}
