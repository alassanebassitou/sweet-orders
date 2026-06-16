package com.devcrafter.Patisserie.App.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class OrderedProductRequest {

    @NotNull
    private Long productId;

    @Min(1)
    private Integer quantity = 1;
    private List<Long> selectedCustomizationIds;

    private Map<String, Object> customizationJson;
    private String cakeMessage;
    private String allergen;
}
