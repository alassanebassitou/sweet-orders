package com.devcrafter.Patisserie.App.models;

import com.devcrafter.Patisserie.App.enums.CommandStatus;
import com.devcrafter.Patisserie.App.enums.DeliveryMode;
import com.devcrafter.Patisserie.App.enums.OrderSource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commande")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Commande extends MainEntity{

    @Column(unique = true)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Enumerated(EnumType.STRING)
    private CommandStatus status = CommandStatus.DRAFT;

    private LocalDateTime commandDate;

    @Column(nullable = false)
    private LocalDate wishDeliveryDate;
    private String creneauHoraire;

    @Enumerated(EnumType.STRING)
    private DeliveryMode deliveryMode = DeliveryMode.HOME_DELIVERY;

    @Column(columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(columnDefinition = "TEXT")
    private String deliveryInstruction;

    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal requireAccount = BigDecimal.ZERO;
    private OrderSource source;

    @Column(columnDefinition = "TEXT")
    private String internalNote;
    private Boolean isEmergency = false;

    @Column(name = "frais_livraison_non_defini")
    private Boolean isDeliveryFeesApplied = false;

    @Column(name = "frais_livraison_paye")
    private Boolean isDeliveryFeesPayed = false;

    @OneToMany(mappedBy = "commande",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<OrderedProducts> orderedProducts = new ArrayList<>();

    @OneToMany(mappedBy = "commande",
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("updatedAt DESC")
    private List<CommandeHistoricStatus> historicStatuses = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_zone_id")
    private DeliveryZone deliveryZone;
}
