package com.devcrafter.Patisserie.App.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SignUpRequest {

    @NotBlank(message = "Le nom est requis")
    private String lastname;

    @NotBlank(message = "Le prénom est requis")
    private String firstname;

    @NotNull(message = "La date de naissance est requise")
    private LocalDate birthday;

    @NotBlank(message = "Le téléphone est requis")
    private String phone;

    @NotBlank(message = "L'email est requis")
    @Email(message = "Email invalide")
    private String email;
}
