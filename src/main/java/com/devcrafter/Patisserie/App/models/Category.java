package com.devcrafter.Patisserie.App.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Category extends MainEntity{

    @Column(unique = true)
    private String name;
    private String description;
    private String photoUrl;
}
