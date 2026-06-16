package com.devcrafter.Patisserie.App.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailLoginRequest {

    @NotBlank(message = "L'email est requis")
    @Email(message = "Email invalide")
    private String email;
}
