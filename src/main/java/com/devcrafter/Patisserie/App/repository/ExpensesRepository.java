package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.dto.response.ExpenseResponse;
import com.devcrafter.Patisserie.App.enums.ExpensesCategory;
import com.devcrafter.Patisserie.App.models.Expenses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpensesRepository extends JpaRepository<Expenses, Long> {

    List<Expenses> findByExpenseDateBetweenOrderByExpenseDateDesc(
            LocalDate start, LocalDate end
    );

    List<Expenses> findByCategoryOrderByExpenseDateDesc(
            ExpensesCategory category
    );

    @Query("SELECT COALESCE(SUM(d.amount), 0) " +
            "FROM Expenses d " +
            "WHERE d.expenseDate BETWEEN :start AND :end")
    BigDecimal totalExpensesPeriod(
            @Param("start") LocalDate start,
            @Param("end")   LocalDate end
    );

    List<Expenses> findAllByCommandeId(Long commandeId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) " +
            "FROM Expenses e WHERE e.commande.id = :commandeId")
    BigDecimal totalExpensesByCommande(
            @Param("commandeId") Long commandeId
    );
}
