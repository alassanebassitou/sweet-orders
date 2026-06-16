package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.dto.response.InvoiceResponse;
import com.devcrafter.Patisserie.App.models.Invoice;
import com.devcrafter.Patisserie.App.services.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Factures", description = "Gestion et téléchargement des factures des commandes")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invoices")
@SecurityRequirement(name = "BearerAuth")
public class InvoiceResource {

    private final InvoiceService invoiceService;

    @Operation(summary = "Lister les factures d'une commande",
            description = "Retourne la liste de toutes les factures associées à un identifiant de commande spécifique.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des factures récupérée"),
            @ApiResponse(responseCode = "404", description = "Commande introuvable")
    })
    @GetMapping("/commande/{commandeId}")
    public ResponseEntity<List<InvoiceResponse>>
    getByCommande(@PathVariable Long commandeId) {
        return ResponseEntity.ok(
                invoiceService.getByCommande(commandeId)
        );
    }

    @Operation(summary = "Télécharger le PDF d'une facture",
            description = "Génère et retourne le fichier PDF d'une facture spécifique pour le téléchargement.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichier PDF généré avec succès",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "404", description = "Facture introuvable")
    })
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long id) {

        byte[] bytes = invoiceService.getPdfBytes(id);
        Invoice invoice = invoiceService.findById(id);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition",
                        "attachment; filename=\"" +
                                invoice.getNumero() + ".pdf\"")
                .body(bytes);
    }
}
