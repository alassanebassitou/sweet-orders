package com.devcrafter.Patisserie.App.models;

import com.devcrafter.Patisserie.App.enums.ExpensesCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Expenses extends MainEntity{

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpensesCategory category;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Commande commande;

}
