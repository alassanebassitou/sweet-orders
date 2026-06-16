package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.Payments;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {

    private Long id;
    private Long commandeId;
    private String numeroCommande;
    private String clientName;
    private BigDecimal amount;
    private String paymentMode;
    private String paymentType;
    private LocalDate paymentDate;
    private String notes;
    private BigDecimal soldeRestant;
    private LocalDateTime createdAt;

    public static PaymentResponse from(Payments p,
                                       BigDecimal soldeRestant) {
        PaymentResponse r = new PaymentResponse();
        r.setId(p.getId());
        r.setCommandeId(p.getCommande().getId());
        r.setNumeroCommande(p.getCommande().getNumero());
        r.setClientName(
                p.getCommande().getClient().getLastname()
                        + " " +
                        p.getCommande().getClient().getFirstname()
        );
        r.setAmount(p.getAmount());
        r.setPaymentMode(p.getPaymentMode().name());
        r.setPaymentType(p.getPaymentType().name());
        r.setPaymentDate(p.getPaymentDate());
        r.setNotes(p.getNotes());
        r.setSoldeRestant(soldeRestant);
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }
}
