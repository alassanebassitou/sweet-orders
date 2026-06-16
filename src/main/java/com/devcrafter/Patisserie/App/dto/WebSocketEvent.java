package com.devcrafter.Patisserie.App.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketEvent {

    private String type;       // COMMANDE_CREATED, STATUT_CHANGED...
    private Object payload;    // Any data
    private LocalDateTime timestamp = LocalDateTime.now();

    public WebSocketEvent(String type, Object payload) {
        this.type      = type;
        this.payload   = payload;
        this.timestamp = LocalDateTime.now();
    }
}
