package com.devcrafter.Patisserie.App.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCustomizationRequest {
    private String libelle;
    private BigDecimal additionalPrice = BigDecimal.ZERO;
    private Boolean isRequire = false;
}
