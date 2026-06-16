package com.devcrafter.Patisserie.App.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeliveryZoneRequest {

    @NotBlank(message = "La ville est requise")
    private String name;

    @NotBlank(message = "Le quartier est requis")
    private String neighborhood;
    private String description;
    private BigDecimal deliveryFees;
}
