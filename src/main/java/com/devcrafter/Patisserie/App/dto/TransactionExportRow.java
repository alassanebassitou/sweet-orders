package com.devcrafter.Patisserie.App.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransactionExportRow {
    private String date;
    private String type;       // REVENU / DEPENSE
    private String category;
    private String description;
    private String amount;
    private String commande;
    private String client;
}
