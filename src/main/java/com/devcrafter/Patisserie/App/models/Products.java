package com.devcrafter.Patisserie.App.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Products extends MainEntity{

    private String name;
    private String description;
    private String photoUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_photos",
            columnDefinition = "jsonb")
    private List<String> additionalPhotos = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    private BigDecimal basePrice;
    private Boolean isActif;

    @OneToMany(mappedBy = "products",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ProductCustomization> customizations;
}
