package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.Products;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ProductResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private String photoUrl;
    private List<String> additionalPhotos;
    private BigDecimal basePrice;
    private String category;
    private Boolean isActif;
    private List<ProductCustomizationResponse> customizationResponses;
    private LocalDateTime createdAt;

    public static ProductResponse from(Products p) {
        ProductResponse r = new ProductResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setDescription(p.getDescription());
        r.setPhotoUrl(p.getPhotoUrl());
        r.setAdditionalPhotos(
                p.getAdditionalPhotos() != null
                        ? p.getAdditionalPhotos()
                        : new ArrayList<>()
        );
        r.setBasePrice(p.getBasePrice());
        r.setCategory(p.getCategory().getName());
        r.setIsActif(p.getIsActif());
        r.setCreatedAt(p.getCreatedAt());
        r.setCustomizationResponses(
                p.getCustomizations() == null
                        ? Collections.emptyList()
                        : p.getCustomizations().stream()
                        .map(ProductCustomizationResponse::from)
                        .toList()
        );
        return r;
    }
}
