package com.devcrafter.Patisserie.App.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class SessionUser implements Serializable {

    private Long userId;
    private String googleSub;
    private String email;
    private String lastname;
    private String firstname;
    private String city;
    private String address;
    private String photoUrl;
    private String role;
    private Boolean isActif;
    private String telephone;
    private LocalDateTime loginAt;
    private LocalDateTime expiresAt;

    public SessionUser(Long userId, String googleSub, String firstname,
                       String email, String lastname, String photoUrl,
                       String role, Boolean isActif, String telephone) {
        this.userId    = userId;
        this.googleSub = googleSub;
        this.email     = email;
        this.lastname = lastname;
        this.firstname = firstname;
        this.photoUrl  = photoUrl;
        this.role      = role;
        this.isActif = isActif;
        this.telephone = telephone;
        this.loginAt   = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusHours(8);
    }

    public SessionUser() {}
}
