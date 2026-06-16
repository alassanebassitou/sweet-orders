package com.devcrafter.Patisserie.App.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "avis",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"client_id", "product_id"}
                // ✅ One review per client per product
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Reviews extends MainEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    // Link to the order that contains this product
    // Ensures client actually bought it before reviewing
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Commande commande;

    // Rating 1 to 5
    @Column(nullable = false)
    @Min(1) @Max(5)
    private Integer note;

    @Column(columnDefinition = "TEXT")
    private String comment;

    // Admin can hide inappropriate reviews
    @Column(name = "is_visible")
    private Boolean isVisible = true;
}
