package com.devcrafter.Patisserie.App.dto.request;

import com.devcrafter.Patisserie.App.enums.CommandStatus;
import lombok.Data;

@Data
public class StatusRequest {
    private CommandStatus status;
    private String commentaire;
}
