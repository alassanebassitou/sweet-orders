package com.devcrafter.Patisserie.App.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyCodeRequest {

    @NotBlank(message = "L'email est requis")
    @Email
    private String email;

    @NotBlank(message = "Le code est requis")
    @Size(min = 6, max = 6, message = "Le code doit avoir 6 chiffres")
    private String code;
}
