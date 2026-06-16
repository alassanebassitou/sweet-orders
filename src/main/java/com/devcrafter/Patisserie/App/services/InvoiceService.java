package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.dto.response.InvoiceResponse;
import com.devcrafter.Patisserie.App.enums.InvoiceType;
import com.devcrafter.Patisserie.App.enums.PaymentType;
import com.devcrafter.Patisserie.App.models.*;
import com.devcrafter.Patisserie.App.repository.CommandeRepository;
import com.devcrafter.Patisserie.App.repository.InvoiceRepository;
import com.devcrafter.Patisserie.App.repository.SettingsRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;


@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SettingsRepository settingsRepository;
    private final EmailService emailService;
    private final CommandeRepository commandeRepository;

    @Value("${app.mail.app-name:Sweet Orders}")
    private String appName;

    @Value("${app.mail.app-url:http://localhost:8090}")
    private String appUrl;

    private static final DateTimeFormatter DATE_FR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private static final DateTimeFormatter DATETIME_FR =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm", Locale.FRENCH
            );

    // Primary caramel color
    private static final Color CARAMEL = new Color(201, 123, 90);
    private static final Color DARK    = new Color(61, 43, 31);
    private static final Color LIGHT   = new Color(253, 248, 244);
    private static final Color GRAY    = new Color(120, 100, 90);

    // ─── Public entry point ───────────────────────────────

    /**
     * Called after every payment is recorded.
     * Generates the invoice PDF and sends it by email.
     */
    // InvoiceService.java

    @Async
    @Transactional
    public void generateAndSendAsync(
            Payments payment,
            Long commandeId,
            BigDecimal montantDejaPaye) {

        try {
            Commande commande = commandeRepository
                    .findByIdWithDetails(commandeId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Commande", commandeId
                    ));

            Invoice invoice = generateAndSend(
                    payment, commande, montantDejaPaye
            );

            if (invoice != null) {
                log.info("Invoice {} generated for order {}",
                        invoice.getNumero(), commande.getNumero());
            }
        } catch (Exception e) {
            log.error("Async invoice generation failed " +
                            "for commande {}: {}",
                    commandeId, e.getMessage(), e);
        }
    }


    private Invoice generateAndSend(
            Payments payment,
            Commande commande,
            BigDecimal montantDejaPaye) {

        try {
            // 1. Determine invoice type
            InvoiceType type = determineType(
                    payment, commande, montantDejaPaye
            );

            // 2. Compute financial values
            BigDecimal montantFacture = payment.getAmount();
            BigDecimal montantTotal   = computeGrandTotal(commande);
            BigDecimal totalPayeApres = montantDejaPaye.add(montantFacture);
            BigDecimal soldeApres = montantTotal
                    .subtract(totalPayeApres)
                    .max(BigDecimal.ZERO);

            // 3. Generate invoice number
            long seq = invoiceRepository
                    .countByCommandeId(commande.getId()) + 1;
            String numero = String.format(
                    "INV-%s-%s-%03d",
                    LocalDateTime.now().getYear(),
                    commande.getNumero().replace("CMD-", ""),
                    seq
            );

            // 4. Generate PDF bytes
            byte[] pdfBytes = generatePdf(
                    numero, type, payment, commande,
                    montantFacture, montantTotal,
                    montantDejaPaye, soldeApres
            );

            // 5. Save invoice to DB
            Invoice invoice = new Invoice();
            invoice.setNumero(numero);
            invoice.setCommande(commande);
            invoice.setPayment(payment);
            invoice.setType(type);
            invoice.setInvoiceAmount(montantFacture);
            invoice.setTotalAmount(montantTotal);
            invoice.setAmountAlreadyPay(montantDejaPaye);
            invoice.setAfterSold(soldeApres);
            invoice.setSubmitDate(LocalDateTime.now());
            invoice.setPdfBase64(
                    Base64.getEncoder().encodeToString(pdfBytes)
            );
            invoice.setEmailSending(false);

            Invoice saved = invoiceRepository.save(invoice);

            // 6. Send by email
            emailService.sendInvoice(commande, saved, pdfBytes);

            saved.setEmailSending(true);
            saved.setEmailSendingAt(LocalDateTime.now());
            invoiceRepository.save(saved);

            log.info("Invoice {} generated and sent for order {}",
                    numero, commande.getNumero());

            return saved;

        } catch (Exception e) {
            log.error("Invoice generation failed for order {}: {}",
                    commande.getNumero(), e.getMessage(), e);
            // Never block the payment flow
            return null;
        }
    }

    // ─── PDF Generation ───────────────────────────────────

    private byte[] generatePdf(
            String numero,
            InvoiceType type,
            Payments payment,
            Commande commande,
            BigDecimal montantFacture,
            BigDecimal montantTotal,
            BigDecimal montantDejaPaye,
            BigDecimal soldeApres) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5, 30, 30, 30, 30);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        // ── HEADER ────────────────────────────────────────
        addHeader(doc, numero, type);

        // ── CLIENT INFO ───────────────────────────────────
        addClientSection(doc, commande);

        // ── INVOICE TITLE ─────────────────────────────────
        addInvoiceTitle(doc, type, numero, payment.getPaymentDate().toString());

        // ── PRODUCTS TABLE ────────────────────────────────
        addProductsTable(doc, commande);

        // ── DELIVERY FEE (if applicable) ──────────────────
        if (type == InvoiceType.FRAIS_LIVRAISON
                && commande.getDeliveryZone().getDeliveryFees() != null
                && commande.getDeliveryZone().getDeliveryFees()
                .compareTo(BigDecimal.ZERO) > 0) {
            addDeliveryFeeSection(doc, commande);
        }

        // ── FINANCIAL SUMMARY ─────────────────────────────
        addFinancialSummary(
                doc, montantTotal, montantDejaPaye,
                montantFacture, soldeApres,
                payment.getPaymentMode().name()
        );

        // ── FOOTER ────────────────────────────────────────
        addFooter(doc);

        doc.close();
        return baos.toByteArray();
    }

    private void addHeader(Document doc,
                           String numero,
                           InvoiceType type) throws Exception {

        // Logo + App name side by side
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1f, 3f});
        headerTable.setSpacingAfter(10f);

        // Logo cell
        try {
            ClassPathResource logoRes =
                    new ClassPathResource("static/favicon.ico");
            Image logo = Image.getInstance(
                    logoRes.getContentAsByteArray()
            );
            logo.scaleToFit(50, 50);
            PdfPCell logoCell = new PdfPCell(logo, false);
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            logoCell.setPaddingRight(8);
            headerTable.addCell(logoCell);
        } catch (Exception e) {
            // Fallback — empty cell if logo not found
            PdfPCell empty = new PdfPCell(new Phrase(""));
            empty.setBorder(Rectangle.NO_BORDER);
            headerTable.addCell(empty);
        }

        // App name + tagline
        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        String appNameVal = getParam("nom_patisserie", appName);
        Paragraph appNameP = new Paragraph(
                appNameVal,
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 16, CARAMEL
                )
        );
        nameCell.addElement(appNameP);

        Paragraph tagline = new Paragraph(
                "Pâtisserie Artisanale — " + getParam("adresse", "Cotonou, Bénin"),
                FontFactory.getFont(FontFactory.HELVETICA, 8, GRAY)
        );
        nameCell.addElement(tagline);

        Paragraph contact = new Paragraph(
                "Tél: " + getParam("telephone_whatsapp", "")
                        + " | " + getParam("email", ""),
                FontFactory.getFont(FontFactory.HELVETICA, 8, GRAY)
        );
        nameCell.addElement(contact);

        headerTable.addCell(nameCell);
        doc.add(headerTable);

        // Horizontal separator line
        LineSeparator line = new LineSeparator();
        line.setLineColor(CARAMEL);
        line.setLineWidth(1.5f);
        doc.add(new Chunk(line));
        doc.add(Chunk.NEWLINE);
    }

    private void addClientSection(Document doc,
                                  Commande commande)
            throws Exception {

        User client = commande.getClient();
        LocalDateTime now = LocalDateTime.now();

        PdfPTable clientTable = new PdfPTable(2);
        clientTable.setWidthPercentage(100);
        clientTable.setSpacingAfter(10f);
        clientTable.setSpacingBefore(5f);

        // Left — client info
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph("FACTURÉ À",
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 7, GRAY
                )
        ));
        left.addElement(new Paragraph(
                client.getFirstname() + " " + client.getLastname(),
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 10, DARK
                )
        ));
        if (client.getTelephone() != null) {
            left.addElement(new Paragraph(
                    "Tél: " + client.getTelephone(),
                    FontFactory.getFont(FontFactory.HELVETICA, 8, GRAY)
            ));
        }
        left.addElement(new Paragraph(
                client.getEmail(),
                FontFactory.getFont(FontFactory.HELVETICA, 8, GRAY)
        ));
        if (commande.getDeliveryZone() != null) {
            left.addElement(new Paragraph(
                    commande.getDeliveryZone().getNeighborhood() + ", " + commande.getDeliveryZone().getName(),
                    FontFactory.getFont(FontFactory.HELVETICA, 8, GRAY)
            ));
        }
        clientTable.addCell(left);

        // Right — invoice meta
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);

        right.addElement(new Paragraph("DATE D'ÉMISSION",
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 7, GRAY
                )
        ));
        right.addElement(new Paragraph(
                DATETIME_FR.format(now),
                FontFactory.getFont(FontFactory.HELVETICA, 9, DARK)
        ));
        right.addElement(Chunk.NEWLINE);
        right.addElement(new Paragraph("COMMANDE",
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 7, GRAY
                )
        ));
        right.addElement(new Paragraph(
                commande.getNumero(),
                FontFactory.getFont(FontFactory.HELVETICA, 9, DARK)
        ));
        right.addElement(new Paragraph(
                "Livraison: " + DATE_FR.format(
                        commande.getWishDeliveryDate()
                ),
                FontFactory.getFont(FontFactory.HELVETICA, 8, GRAY)
        ));
        clientTable.addCell(right);

        doc.add(clientTable);
    }

    private void addInvoiceTitle(Document doc,
                                 InvoiceType type,
                                 String numero,
                                 String date)
            throws Exception {

        // Colored title band
        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingBefore(5f);
        titleTable.setSpacingAfter(10f);

        String label = switch (type) {
            case ACOMPTE         -> "FACTURE D'ACOMPTE";
            case SOLDE           -> "FACTURE DE SOLDE";
            case FRAIS_LIVRAISON -> "FACTURE — FRAIS DE LIVRAISON";
            case INTEGRAL        -> "FACTURE — PAIEMENT INTÉGRAL";
        };

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(CARAMEL);
        titleCell.setPadding(8);
        titleCell.setBorder(Rectangle.NO_BORDER);

        Paragraph title = new Paragraph(
                label,
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 13,
                        Color.WHITE
                )
        );
        title.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(title);

        Paragraph numP = new Paragraph(
                "N° " + numero,
                FontFactory.getFont(FontFactory.HELVETICA, 9,
                        new Color(255, 220, 200))
        );
        numP.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(numP);

        titleTable.addCell(titleCell);
        doc.add(titleTable);
    }

    private void addProductsTable(Document doc, Commande commande)
            throws Exception {

        // Table header
        Paragraph sectionTitle = new Paragraph(
                "DÉTAIL DE LA COMMANDE",
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 8, GRAY
                )
        );
        sectionTitle.setSpacingBefore(5f);
        sectionTitle.setSpacingAfter(4f);
        doc.add(sectionTitle);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1f, 1.5f, 1.5f});
        table.setSpacingAfter(10f);

        // Table headers
        String[] headers = {"Produit", "Qté", "Prix unit.", "Total"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(
                    new Phrase(h,
                            FontFactory.getFont(
                                    FontFactory.HELVETICA_BOLD, 8,
                                    Color.WHITE
                            )
                    )
            );
            cell.setBackgroundColor(DARK);
            cell.setPadding(5f);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }

        // Product rows
        boolean alternate = false;
        for (OrderedProducts op : commande.getOrderedProducts()) {
            Color rowBg = alternate ? LIGHT : Color.WHITE;
            alternate = !alternate;

            // Product name + details
            PdfPCell nameCell = new PdfPCell();
            nameCell.setBackgroundColor(rowBg);
            nameCell.setBorder(Rectangle.NO_BORDER);
            nameCell.setPadding(5f);

            nameCell.addElement(new Paragraph(
                    op.getProducts().getName(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, DARK)
            ));

            // Personalizations
            if (op.getCakeMessage() != null
                    && !op.getCakeMessage().isBlank()) {
                nameCell.addElement(new Paragraph(
                        "  Message: " + op.getCakeMessage(),
                        FontFactory.getFont(
                                FontFactory.HELVETICA, 7, GRAY
                        )
                ));
            }

            if (op.getCustomizationJson() != null
                    && !op.getCustomizationJson().isEmpty()) {
                String perso = String.join(", ",
                        op.getCustomizationJson().keySet()
                );
                nameCell.addElement(new Paragraph(
                        "  Options: " + perso,
                        FontFactory.getFont(
                                FontFactory.HELVETICA, 7, GRAY
                        )
                ));
            }

            table.addCell(nameCell);

            // Quantity
            table.addCell(createDataCell(
                    String.valueOf(op.getQuantity()),
                    rowBg, Element.ALIGN_CENTER
            ));

            // Unit price
            table.addCell(createDataCell(
                    fcfa(op.getUnitPrice()), rowBg, Element.ALIGN_RIGHT
            ));

            // Total
            table.addCell(createDataCell(
                    fcfa(op.getTotalPrice()), rowBg, Element.ALIGN_RIGHT
            ));
        }

        doc.add(table);
    }

    private void addDeliveryFeeSection(Document doc,
                                       Commande commande)
            throws Exception {

        PdfPTable feeTable = new PdfPTable(2);
        feeTable.setWidthPercentage(100);
        feeTable.setSpacingAfter(8f);

        PdfPCell label = new PdfPCell(new Phrase(
                "🚚 Frais de livraison\n"
                        + "   " + commande.getDeliveryZone().getNeighborhood()
                        + " (" + commande.getDeliveryZone().getNeighborhood() + ")",
                FontFactory.getFont(FontFactory.HELVETICA, 8, DARK)
        ));
        label.setBorder(Rectangle.NO_BORDER);
        label.setPadding(5f);
        label.setBackgroundColor(LIGHT);

        PdfPCell amount = new PdfPCell(new Phrase(
                fcfa(commande.getDeliveryZone().getDeliveryFees()),
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 8, CARAMEL
                )
        ));
        amount.setBorder(Rectangle.NO_BORDER);
        amount.setPadding(5f);
        amount.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amount.setBackgroundColor(LIGHT);

        feeTable.addCell(label);
        feeTable.addCell(amount);
        doc.add(feeTable);
    }

    private void addFinancialSummary(
            Document doc,
            BigDecimal montantTotal,
            BigDecimal montantDejaPaye,
            BigDecimal montantFacture,
            BigDecimal soldeApres,
            String modePaiement) throws Exception {

        // Separator
        LineSeparator sep = new LineSeparator();
        sep.setLineColor(CARAMEL);
        sep.setLineWidth(0.5f);
        doc.add(new Chunk(sep));
        doc.add(Chunk.NEWLINE);

        PdfPTable summary = new PdfPTable(2);
        summary.setWidthPercentage(65);
        summary.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summary.setSpacingBefore(5f);
        summary.setSpacingAfter(10f);

        // Total commande
        addSummaryRow(summary, "Montant total commande",
                fcfa(montantTotal), false);

        // Previous payments (only if partial)
        if (montantDejaPaye.compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(summary, "Déjà payé",
                    fcfa(montantDejaPaye), false);
        }

        // This payment — highlighted
        addSummaryRow(summary,
                "Montant payé (ce paiement)",
                fcfa(montantFacture), true);

        // Mode de paiement
        String modeLabel = switch (modePaiement) {
            case "CASH"         -> "Espèces";
            case "MOBILE_MONEY" -> "Mobile Money";
            case "KKIAPAY"      -> "Kkiapay (Mobile Money/Carte)";
            case "CARD"         -> "Virement bancaire";
            default             -> modePaiement;
        };
        addSummaryRow(summary, "Mode de paiement",
                modeLabel, false);

        // Remaining balance
        if (soldeApres.compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(summary, "Solde restant",
                    fcfa(soldeApres), false);
        } else {
            addSummaryRow(summary, "Solde restant",
                    "0 FCFA ✓", false);
        }

        doc.add(summary);

        // Paid in full badge
        if (soldeApres.compareTo(BigDecimal.ZERO) == 0) {
            PdfPTable paidTable = new PdfPTable(1);
            paidTable.setWidthPercentage(60);
            paidTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell paidCell = new PdfPCell(
                    new Phrase("✓ PAYÉ INTÉGRALEMENT",
                            FontFactory.getFont(
                                    FontFactory.HELVETICA_BOLD, 9,
                                    Color.WHITE
                            )
                    )
            );
            paidCell.setBackgroundColor(new Color(76, 175, 80));
            paidCell.setPadding(6f);
            paidCell.setBorder(Rectangle.NO_BORDER);
            paidCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            paidTable.addCell(paidCell);
            doc.add(paidTable);
        } else {
            PdfPTable pendingTable = new PdfPTable(1);
            pendingTable.setWidthPercentage(60);
            pendingTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell pendingCell = new PdfPCell(
                    new Phrase("⚠ SOLDE EN ATTENTE",
                            FontFactory.getFont(
                                    FontFactory.HELVETICA_BOLD, 9,
                                    Color.WHITE
                            )
                    )
            );
            pendingCell.setBackgroundColor(new Color(230, 160, 50));
            pendingCell.setPadding(6f);
            pendingCell.setBorder(Rectangle.NO_BORDER);
            pendingCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            pendingTable.addCell(pendingCell);
            doc.add(pendingTable);
        }
    }

    private void addFooter(Document doc) throws Exception {

        doc.add(Chunk.NEWLINE);
        LineSeparator sep = new LineSeparator();
        sep.setLineColor(new Color(220, 200, 190));
        sep.setLineWidth(0.5f);
        doc.add(new Chunk(sep));
        doc.add(Chunk.NEWLINE);

        Paragraph footer = new Paragraph(
                "Merci pour votre confiance ! 🎂\n"
                        + appName + " — "
                        + getParam("adresse", "Cotonou, Bénin") + "\n"
                        + getParam("telephone_whatsapp", "")
                        + " | " + appUrl,
                FontFactory.getFont(FontFactory.HELVETICA, 7, GRAY)
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        // Legal note
        Paragraph legal = new Paragraph(
                "Cette facture est générée automatiquement par "
                        + appName + ". Document valable comme reçu de paiement.",
                FontFactory.getFont(FontFactory.HELVETICA, 6,
                        new Color(180, 160, 150))
        );
        legal.setAlignment(Element.ALIGN_CENTER);
        legal.setSpacingBefore(4f);
        doc.add(legal);
    }

    // ─── Helpers ──────────────────────────────────────────

    private PdfPCell createDataCell(String text,
                                    Color bg,
                                    int align) {
        PdfPCell cell = new PdfPCell(
                new Phrase(text,
                        FontFactory.getFont(FontFactory.HELVETICA, 8, DARK)
                )
        );
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(align);
        return cell;
    }

    private void addSummaryRow(PdfPTable table,
                               String label,
                               String value,
                               boolean highlight) {
        Font labelFont = highlight
                ? FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 9, DARK)
                : FontFactory.getFont(
                FontFactory.HELVETICA, 8, GRAY);

        Font valueFont = highlight
                ? FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 10, CARAMEL)
                : FontFactory.getFont(
                FontFactory.HELVETICA, 8, DARK);

        Color bg = highlight ? LIGHT : Color.WHITE;

        PdfPCell labelCell = new PdfPCell(
                new Phrase(label, labelFont)
        );
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4f);
        labelCell.setBackgroundColor(bg);

        PdfPCell valueCell = new PdfPCell(
                new Phrase(value, valueFont)
        );
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(4f);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBackgroundColor(bg);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private InvoiceType determineType(
            Payments payment,
            Commande commande,
            BigDecimal montantDejaPaye) {

        if (payment.getPaymentType() == PaymentType.DELIVERY_FEES) {
            return InvoiceType.FRAIS_LIVRAISON;
        }

        BigDecimal grandTotal = computeGrandTotal(commande);
        BigDecimal totalApres = montantDejaPaye
                .add(payment.getAmount());

        if (montantDejaPaye.compareTo(BigDecimal.ZERO) == 0) {
            // First payment
            if (totalApres.compareTo(grandTotal) >= 0) {
                return InvoiceType.INTEGRAL;
            }
            return InvoiceType.ACOMPTE;
        } else {
            // Subsequent payment — paying remaining balance
            return InvoiceType.SOLDE;
        }
    }

    private BigDecimal computeGrandTotal(Commande commande) {
        BigDecimal total = commande.getTotalAmount() != null
                ? commande.getTotalAmount()
                : BigDecimal.ZERO;

        if (commande.getDeliveryZone() != null
                && commande.getDeliveryZone().getDeliveryFees() != null
                && Boolean.TRUE.equals(commande.getIsDeliveryFeesPayed())) {
            total = total.add(
                    commande.getDeliveryZone().getDeliveryFees()
            );
        }

        return total;
    }

    private String fcfa(BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        NumberFormat nf = NumberFormat.getInstance(Locale.FRANCE);
        nf.setMaximumFractionDigits(0);
        return nf.format(amount) + " FCFA";
    }

    private String getParam(String key, String defaultVal) {
        return settingsRepository.findByCle(key)
                .map(Settings::getValue)
                .orElse(defaultVal);
    }

    // ─── Download PDF ─────────────────────────────────────

    public byte[] getPdfBytes(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Facture", invoiceId)
                );
        return Base64.getDecoder().decode(invoice.getPdfBase64());
    }

    // ─── Get invoices for a commande ──────────────────────

    public List<InvoiceResponse> getByCommande(Long commandeId) {
        return invoiceRepository
                .findByCommandeIdOrderByCreatedAtDesc(commandeId)
                .stream()
                .map(InvoiceResponse::from)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Invoice findById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", id));
    }
}
