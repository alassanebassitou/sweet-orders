package com.devcrafter.Patisserie.App.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderedProducts extends MainEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "command_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Products products;

    @Column(nullable = false)
    private Integer quantity = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "customization_json", columnDefinition = "jsonb")
    private Map<String, Object> customizationJson;

    private String cakeMessage;
    private String photoUrl;
    private String allergen;

    @Column(nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private Boolean isFinish = false;
}
