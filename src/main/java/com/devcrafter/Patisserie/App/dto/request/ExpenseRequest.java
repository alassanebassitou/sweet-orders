package com.devcrafter.Patisserie.App.dto.request;

import com.devcrafter.Patisserie.App.enums.ExpensesCategory;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseRequest {

    @NotNull
    private ExpensesCategory category;

    @NotNull
    private BigDecimal amount;

    private String description;

    @NotNull
    private LocalDate expenseDate;

    private Long commandeId;
}
