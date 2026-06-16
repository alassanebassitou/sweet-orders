package com.devcrafter.Patisserie.App.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin")
@DiscriminatorValue("ADMIN")
public class Admin extends User{
}
