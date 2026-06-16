package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.Invoice;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InvoiceResponse {
    private Long id;
    private String numero;
    private String commandeNumero;
    private String type;
    private BigDecimal invoiceAmount;
    private BigDecimal totalAmount;
    private BigDecimal amountAlreadyPay;
    private BigDecimal afterSold;
    private LocalDateTime submitDate;
    private Boolean emailSending;
    private String downloadUrl;

    public static InvoiceResponse from(Invoice i) {
        InvoiceResponse r = new InvoiceResponse();
        r.setId(i.getId());
        r.setNumero(i.getNumero());
        r.setCommandeNumero(i.getCommande().getNumero());
        r.setType(i.getType().name());
        r.setInvoiceAmount(i.getInvoiceAmount());
        r.setTotalAmount(i.getTotalAmount());
        r.setAmountAlreadyPay(i.getAmountAlreadyPay());
        r.setAfterSold(i.getAfterSold());
        r.setSubmitDate(i.getSubmitDate());
        r.setEmailSending(i.getEmailSending());
        r.setDownloadUrl("/api/v1/invoices/" + i.getId() + "/pdf");
        return r;
    }
}
