package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.CommandeHistoricStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommandeHistoricResponse {

    private String oldStatut;
    private String newStatut;
    private String commentaire;
    private String changedByEmail;
    //private LocalDateTime changedAt;

    public static CommandeHistoricResponse from(
            CommandeHistoricStatus h) {
        CommandeHistoricResponse r = new CommandeHistoricResponse();
        r.setOldStatut(
                h.getOldStatus() != null
                        ? h.getOldStatus().name() : null);
        r.setNewStatut(h.getNewStatus().name());
        r.setCommentaire(h.getCommentaire());
        r.setChangedByEmail(h.getUpdatedBy());
        //r.setChangedAt(h.getUpdatedAt());
        return r;
    }
}
