package com.devcrafter.Patisserie.App.dto.request;

import com.devcrafter.Patisserie.App.enums.DeliveryMode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CommandeRequest {

    @NotNull
    private LocalDate wishDeliveryDate;
    private String creneauHoraire;
    private DeliveryMode deliveryMode;
    private String deliveryAddress;
    private String deliveryInstruction;
    private Boolean isEmergency = false;
    private String source;

    private String city;
    private String neighborhood;
    private BigDecimal deliveryFees;
    private Boolean isDeliveryFeesApplied;

    @NotEmpty
    private List<OrderedProductRequest> productRequests;
}
