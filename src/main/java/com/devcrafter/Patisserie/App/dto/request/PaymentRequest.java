package com.devcrafter.Patisserie.App.dto.request;

import com.devcrafter.Patisserie.App.enums.PaymentMode;
import com.devcrafter.Patisserie.App.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentRequest {

    @NotNull
    private Long commandeId;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private PaymentMode paymentMode;

    @NotNull
    private PaymentType paymentType;

    @NotNull
    private LocalDate paymentDate;

    private String notes;
    private String transactionId;
}
