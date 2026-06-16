package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.ProductCustomization;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCustomizationResponse {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String libelle;
    private BigDecimal additionalPrice;
    private Boolean isRequire;

    public static ProductCustomizationResponse from(ProductCustomization p) {
        ProductCustomizationResponse r = new ProductCustomizationResponse();
        r.setId(p.getId());
        r.setLibelle(p.getLibelle());
        r.setAdditionalPrice(p.getAdditionalPrice());
        r.setIsRequire(p.getIsRequire());
        return r;
    }
}
