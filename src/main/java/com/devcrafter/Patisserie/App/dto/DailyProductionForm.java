package com.devcrafter.Patisserie.App.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DailyProductionForm {
    private LocalDate date;
    private int totalCakes;
    private boolean overload;
    private List<LineProduction> lines;

    @Data
    @AllArgsConstructor
    public static class LineProduction {
        private Long commandeId;
        private String noCommande;
        private String clientName;
        private String clientPhoneNumber;
        private String productName;
        private Integer quantity;
        private String cakeMessage;
        private String customizations;
        private Boolean isFinished;
    }
}
