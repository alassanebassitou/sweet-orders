package com.devcrafter.Patisserie.App.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    private String name;
    private String description;
    private BigDecimal basePrice;
    private String category;
}
