package com.devcrafter.Patisserie.App.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryRequest {

    @NotNull
    private Long commandeId;

    @NotNull
    private LocalDate expectedDate;

    private String expectedHour;

    private String notes;
}
