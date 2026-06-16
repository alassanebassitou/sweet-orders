package com.devcrafter.Patisserie.App.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneRequest {
    @NotBlank(message = "Le téléphone est requis")
    private String telephone;
}
