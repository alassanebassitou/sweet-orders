package com.devcrafter.Patisserie.App.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "client")
@DiscriminatorValue("CLIENT")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Client extends User{

    private String address;
    private String city;
    private String note;
    private LocalDate birthDay;
    private Boolean isVIP;
    private BigDecimal remiseVIP;
}
