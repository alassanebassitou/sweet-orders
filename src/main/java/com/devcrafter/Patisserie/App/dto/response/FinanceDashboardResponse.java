package com.devcrafter.Patisserie.App.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class FinanceDashboardResponse {

    // Revenue
    private BigDecimal monthlyRevenue;
    private BigDecimal yearlyRevenue;
    private BigDecimal totalRevenue;

    // Payments
    private BigDecimal totalReceivedPayments;
    private BigDecimal totalRemainingBalances;

    // Expenses
    private BigDecimal monthlyExpenses;
    private BigDecimal yearlyExpenses;

    // Profit
    private BigDecimal netProfitMonthly;
    private BigDecimal netProfitYearly;

    // Orders
    private long totalOrders;
    private long paidOrders;
    private long partiallyPaidOrders;
    private long unpaidOrders;

    // Chart data — last 12 months
    private List<MonthlyData> revenueByMonth;
    private List<TopProduitData> topProducts;

    // Expenses by category
    private Map<String, BigDecimal> expensesByCategory;

    @Data
    @AllArgsConstructor
    public static class MonthlyData {
        private String month;
        private BigDecimal revenue;
        private BigDecimal expenses;
        private BigDecimal profit;
    }

    @Data
    @AllArgsConstructor
    public static class TopProduitData {
        private String name;
        private Long commande;
        private BigDecimal revenue;
    }
}
