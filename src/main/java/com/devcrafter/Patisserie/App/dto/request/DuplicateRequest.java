package com.devcrafter.Patisserie.App.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DuplicateRequest {
    private LocalDate wishDeliveryDate;
}
