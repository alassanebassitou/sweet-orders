package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.Expenses;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExpenseResponse {

    private Long id;
    private String category;
    private BigDecimal amount;
    private String description;
    private LocalDate expenseDate;
    private Long commandeId;
    private String noCommande;
    private LocalDateTime createdAt;

    public static ExpenseResponse from(Expenses e) {
        ExpenseResponse r = new ExpenseResponse();
        r.setId(e.getId());
        r.setCategory(e.getCategory().name());
        r.setAmount(e.getAmount());
        r.setDescription(e.getDescription());
        r.setExpenseDate(e.getExpenseDate());
        r.setCreatedAt(e.getCreatedAt());

        if (e.getCommande() != null) {
            r.setCommandeId(e.getCommande().getId());
            r.setNoCommande(e.getCommande().getNumero());
        }
        return r;
    }
}
