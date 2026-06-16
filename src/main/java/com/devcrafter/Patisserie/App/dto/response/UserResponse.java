package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.Client;
import com.devcrafter.Patisserie.App.models.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserResponse {

    private Long id;
    private String email;
    private String lastname;
    private String firstname;
    private String telephone;
    private String photoUrl;
    private String role;
    private LocalDateTime createdAt;

    private String address;
    private String city;
    private String note;
    private LocalDate birthDay;
    private Boolean isVIP;
    private BigDecimal remiseVIP;

    private Integer totalCommande;
    private BigDecimal totalExpenses;
    private Boolean isActif;

    // Static factory — converts User entity to UserResponse DTO
    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setLastname(user.getLastname());
        response.setFirstname(user.getFirstname());
        response.setTelephone(user.getTelephone());
        response.setPhotoUrl(user.getPhotoUrl());
        response.setRole(user.getRole().name());
        response.setCreatedAt(user.getCreatedAt());
        response.setIsActif(user.getIsActif());

        if (user instanceof Client client) {
            response.setAddress(client.getAddress());
            response.setCity(client.getCity());
            response.setNote(client.getNote());
            response.setBirthDay(client.getBirthDay());
            response.setIsVIP(client.getIsVIP());
            response.setRemiseVIP(client.getRemiseVIP());
        }
        return response;
    }
}
