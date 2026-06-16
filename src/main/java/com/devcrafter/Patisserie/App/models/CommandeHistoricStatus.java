package com.devcrafter.Patisserie.App.models;

import com.devcrafter.Patisserie.App.enums.CommandStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommandeHistoricStatus extends MainEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "command_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Commande commande;

    @Enumerated(EnumType.STRING)
    private CommandStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommandStatus newStatus;

    private String commentaire;
}
