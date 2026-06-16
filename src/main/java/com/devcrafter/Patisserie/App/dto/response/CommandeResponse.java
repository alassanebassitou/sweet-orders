package com.devcrafter.Patisserie.App.dto.response;


import com.devcrafter.Patisserie.App.models.Commande;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommandeResponse {
    private Long id;
    private String numero;
    private String clientEmail;
    private String clientName;
    private String clientPhone;
    private String status;

    private LocalDate wishDeliveryDate;
    private String creneauHoraire;
    private String deliveryMode;
    private String deliveryAddress;
    private String deliveryInstruction;

    /*private BigDecimal totalExpense;
    private BigDecimal netProfit;*/

    private BigDecimal totalPaye;
    private BigDecimal netProfit;

    private BigDecimal totalAmount;
    private BigDecimal totalAvecLivraison;
    private BigDecimal requireAccount;
    private Boolean isEmergency;
    private String source;
    private String internalNote;

    private String city;
    private String neighborhood;
    private BigDecimal deliveryFees;
    private Boolean isDeliveryFeesApplied;
    private Boolean isDeliveryFeesPayed;
    private Boolean isFullyPaid;

    private List<OrderedProductResponse> products;
    private List<CommandeHistoricResponse> historique;
    private List<ExpenseResponse> expenses;
    private LocalDateTime createdAt;

    public static CommandeResponse from(Commande c/*, BigDecimal totalExpense*/) {
        CommandeResponse r = new CommandeResponse();
        r.setId(c.getId());
        r.setNumero(c.getNumero());
        r.setClientEmail(c.getClient().getEmail());
        r.setClientName(c.getClient().getLastname()
                + " " + c.getClient().getFirstname());
        r.setClientPhone(c.getClient().getTelephone());
        r.setStatus(c.getStatus().name());
        r.setWishDeliveryDate(c.getWishDeliveryDate());
        r.setCreneauHoraire(c.getCreneauHoraire());
        r.setDeliveryMode(c.getDeliveryMode().name());
        r.setDeliveryAddress(c.getDeliveryAddress());
        r.setDeliveryInstruction(c.getDeliveryInstruction());
        r.setTotalAmount(c.getTotalAmount());
        r.setRequireAccount(c.getRequireAccount());
        r.setIsEmergency(c.getIsEmergency());

        if (c.getSource() != null) {
            r.setSource(c.getSource().name());
        }
        r.setInternalNote(c.getInternalNote());

//        r.setTotalExpense(null);
//        r.setNetProfit(c.getTotalAmount()
//                .subtract(null)
//                .max(BigDecimal.ZERO));

        if (c.getDeliveryZone() != null) {
            r.setCity(c.getDeliveryZone().getName());
            r.setNeighborhood(c.getDeliveryZone().getNeighborhood());
            r.setDeliveryFees(
                    c.getDeliveryZone().getDeliveryFees() != null
                            ? c.getDeliveryZone().getDeliveryFees()
                            : BigDecimal.ZERO
            );
            r.setTotalAvecLivraison(
                    c.getTotalAmount().add(
                            c.getDeliveryZone().getDeliveryFees() != null
                                    ? c.getDeliveryZone().getDeliveryFees()
                                    : BigDecimal.ZERO
                    )
            );
        } else {
            r.setDeliveryFees(BigDecimal.ZERO);
            r.setTotalAvecLivraison(BigDecimal.ZERO);
        }

        r.setIsDeliveryFeesApplied(
                Boolean.TRUE.equals(c.getIsDeliveryFeesApplied())
        );
        r.setIsDeliveryFeesPayed(
                Boolean.TRUE.equals(c.getIsDeliveryFeesPayed())
        );

        r.setProducts(
                c.getOrderedProducts().stream()
                        .map(OrderedProductResponse::from)
                        .toList()
        );
        r.setHistorique(
                c.getHistoricStatuses().stream()
                        .map(CommandeHistoricResponse::from)
                        .toList()
        );
        r.setCreatedAt(c.getCreatedAt());

        r.setTotalPaye(BigDecimal.ZERO);
        r.setNetProfit(c.getTotalAmount() != null
                ? c.getTotalAmount()
                : BigDecimal.ZERO);

        return r;
    }
}
