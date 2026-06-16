package com.devcrafter.Patisserie.App.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReprogramRequest {

    @NotNull
    private LocalDate newDate;
    private String newHour;
    private String notes;
}
