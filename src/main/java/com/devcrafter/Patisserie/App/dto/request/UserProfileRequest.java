package com.devcrafter.Patisserie.App.dto.request;

import lombok.Data;

@Data
public class UserProfileRequest {

    private String lastname;
    private String firstname;
    private String telephone;
    private String address;
    private String city;
}
