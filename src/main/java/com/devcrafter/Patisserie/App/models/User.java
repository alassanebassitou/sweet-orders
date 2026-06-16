package com.devcrafter.Patisserie.App.models;

import com.devcrafter.Patisserie.App.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User extends MainEntity{

    @Column(name = "google_id", unique = true)
    private String googleSub;

    @Column(name = "facebook_id", unique = true)
    private String facebookSub;

    @Column(unique = true, nullable = false)
    private String email;

    private String lastname;
    private String firstname;
    private String photoUrl;
    private String telephone;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Boolean isActif = true;

}
