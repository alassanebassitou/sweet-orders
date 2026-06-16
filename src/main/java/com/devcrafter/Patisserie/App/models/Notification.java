package com.devcrafter.Patisserie.App.models;

import com.devcrafter.Patisserie.App.enums.NotificationType;
import com.devcrafter.Patisserie.App.enums.NotificationTypes;
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
public class Notification extends MainEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "command_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private User client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationTypes type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private Boolean isRead = false;
}
