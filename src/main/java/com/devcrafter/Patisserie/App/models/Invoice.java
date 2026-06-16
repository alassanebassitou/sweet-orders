package com.devcrafter.Patisserie.App.models;

import com.devcrafter.Patisserie.App.enums.InvoiceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
public class Invoice extends MainEntity {

    @Column(unique = true, nullable = false)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payments payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceType type;

    @Column(nullable = false)
    private BigDecimal invoiceAmount;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private BigDecimal amountAlreadyPay;

    @Column(nullable = false)
    private BigDecimal afterSold;

    @Column(nullable = false)
    private LocalDateTime submitDate;

    // PDF stored as base64 or URL
    @Column(columnDefinition = "TEXT")
    private String pdfBase64;

    // Email sent confirmation
    private Boolean emailSending = false;
    private LocalDateTime emailSendingAt;
}
