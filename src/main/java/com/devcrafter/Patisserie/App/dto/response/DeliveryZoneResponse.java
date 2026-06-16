package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.DeliveryZone;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@Data
public class DeliveryZoneResponse {
    private Long id;
    private String name;
    private String neighborhood;
    private String description;
    private BigDecimal deliveryFees;
    private Boolean actif;

    public static DeliveryZoneResponse from(DeliveryZone z) {
        DeliveryZoneResponse r = new DeliveryZoneResponse();
        r.setId(z.getId());
        r.setName(z.getName());
        r.setNeighborhood(z.getNeighborhood());
        r.setDescription(z.getDescription());
        r.setDeliveryFees(z.getDeliveryFees());
        r.setActif(z.getActif());
        return r;
    }
}
