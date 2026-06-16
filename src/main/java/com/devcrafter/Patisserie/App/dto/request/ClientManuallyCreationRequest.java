package com.devcrafter.Patisserie.App.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClientManuallyCreationRequest {

    @NotNull
    private String email;
    private String lastname;
    private String firstname;

    @NotNull
    private String phone;

    @NotNull
    private String address;
    private String city;
}
