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
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TemplateMessage extends MainEntity {

    @Column(unique = true, nullable = false)
    private String type;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}
