package com.devcrafter.Patisserie.App.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KkiapayRequest {

    @NotNull
    private Long commandeId;
    private String successUrl;
    private String failureUrl;
}
