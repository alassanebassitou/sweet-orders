package com.devcrafter.Patisserie.App.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApplyDeliveryFeeRequest {

    @NotNull(message = "Le montant des frais est requis")
    @DecimalMin(value = "1", message = "Les frais doivent être supérieurs à 0")
    private BigDecimal deliveryFees;
    private Long zoneId;
}
