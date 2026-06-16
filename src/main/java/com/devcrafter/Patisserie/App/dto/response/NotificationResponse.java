package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.Notification;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {

    private Long id;
    private String type;
    private String message;
    private Boolean isRead;
    private Long commandeId;
    private String noCommande;
    private Long clientId;
    private String clientName;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setType(n.getType().name());
        r.setMessage(n.getMessage());
        r.setIsRead(n.getIsRead());
        r.setCreatedAt(n.getCreatedAt());

        if (n.getCommande() != null) {
            r.setCommandeId(n.getCommande().getId());
            r.setNoCommande(n.getCommande().getNumero());
        }
        if (n.getClient() != null) {
            r.setClientId(n.getClient().getId());
            r.setClientName(
                    n.getClient().getLastname()
                            + " " +
                            n.getClient().getFirstname()
            );
        }
        return r;
    }
}
