package com.devcrafter.Patisserie.App.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductCustomization extends MainEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Products products;
    private String libelle;
    private BigDecimal additionalPrice = BigDecimal.ZERO;
    private Boolean  isRequire;
}
