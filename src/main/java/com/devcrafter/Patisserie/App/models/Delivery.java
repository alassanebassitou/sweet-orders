package com.devcrafter.Patisserie.App.models;


import com.devcrafter.Patisserie.App.enums.DeliveryStatus;
import com.devcrafter.Patisserie.App.enums.FailureReason;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Delivery extends MainEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Commande commande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus statut = DeliveryStatus.PLANNED;

    @Column(nullable = false)
    private LocalDate expectedDate;

    private String expectedHour;

    private LocalDateTime effectiveDate;

    @Enumerated(EnumType.STRING)
    private FailureReason failureReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String deliveryAddress;
}
