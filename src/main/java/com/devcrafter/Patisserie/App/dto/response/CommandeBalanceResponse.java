package com.devcrafter.Patisserie.App.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommandeBalanceResponse {
    private BigDecimal totalAmount;
    private BigDecimal requireAccount;
    private BigDecimal netProfit;
    private BigDecimal totalPaye;
}
