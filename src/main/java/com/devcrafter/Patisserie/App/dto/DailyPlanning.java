package com.devcrafter.Patisserie.App.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DailyPlanning {
    private LocalDate date;
    private String dayOfWeek;
    private int totalCakes;
    private int totalCommandes;
    private boolean overload;
    private List<CommandeResume> commandes;

    @Data
    @AllArgsConstructor
    public static class CommandeResume {
        private Long id;
        private String numero;
        private String clientName;
        private String status;
        private int numberOfCakes;
    }
}
