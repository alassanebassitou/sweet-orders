package com.devcrafter.Patisserie.App.models;

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
public class MessageTemplate extends MainEntity{

    private String type;
    private String libelle;
    private String content;
}
