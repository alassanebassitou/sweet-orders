package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.OrderedProducts;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class OrderedProductResponse {

    private Long id;
    private String productName;
    private Long productId;
    private String photoUrl;
    private Integer quantity;
    private Map<String, Object> customizationJson;
    private String cakeMessage;
    private String allergen;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Boolean isFinish;

    public static OrderedProductResponse from(OrderedProducts cp) {
        OrderedProductResponse r = new OrderedProductResponse();
        r.setId(cp.getId());
        r.setProductName(cp.getProducts().getName());
        r.setProductId(cp.getProducts().getId());
        r.setPhotoUrl(cp.getProducts().getPhotoUrl());
        r.setQuantity(cp.getQuantity());
        r.setCustomizationJson(cp.getCustomizationJson());
        r.setCakeMessage(cp.getCakeMessage());
        r.setAllergen(cp.getAllergen());
        r.setUnitPrice(cp.getUnitPrice());
        r.setTotalPrice(cp.getTotalPrice());
        r.setIsFinish(cp.getIsFinish());
        return r;
    }
}
