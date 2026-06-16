package com.devcrafter.Patisserie.App.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "delivery_zone",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"name", "quartier"}
        )
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryZone extends MainEntity{

    @Column(nullable = false)
    private String name;

    private String neighborhood;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(
            //name = "delivery_frees",
            columnDefinition = "numeric(38,2) default 0"
    )
    private BigDecimal deliveryFees = BigDecimal.ZERO;
    private Boolean actif = true;
}
