package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.dto.request.PaymentRequest;
import com.devcrafter.Patisserie.App.dto.response.PaymentResponse;
import com.devcrafter.Patisserie.App.models.SessionUser;
import com.devcrafter.Patisserie.App.services.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.devcrafter.Patisserie.App.utils.AppConstants.CURRENT_USER;


@Tag(name = "Paiements", description = "Enregistrement et consultation des paiements")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@SecurityRequirement(name = "BearerAuth")
public class PaymentResource {

    private final PaymentService paymentService;

    @Operation(summary = "Enregistrer un paiement",
            description = "Enregistre manuellement un paiement (espèces, virement...). ADMIN uniquement.")
    @ApiResponse(responseCode = "200", description = "Paiement enregistré",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> recordPayment(
            @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {
        SessionUser user = (SessionUser) httpRequest.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(paymentService.recordPayment(request, user));
    }

    @Operation(summary = "Lister tous les paiements",
            description = "Retourne tous les paiements, filtrables par commande. ADMIN uniquement.")
    @ApiResponse(responseCode = "200", description = "Liste des paiements")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getAllPayments(
            @Parameter(description = "Filtrer par ID de commande (optionnel)")
            @RequestParam(required = false) Long commandeId) {
        return ResponseEntity.ok(paymentService.getAllPayments(commandeId));
    }

    @Operation(summary = "Paiements d'une commande",
            description = "Retourne tous les paiements associés à une commande donnée.")
    @ApiResponse(responseCode = "200", description = "Paiements de la commande")
    @GetMapping("/commandes/{id}")
    public ResponseEntity<List<PaymentResponse>> getAllPaymentsByCommande(
            @Parameter(description = "ID de la commande") @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getAllPaymentsByCommande(id));
    }

    @Operation(summary = "Solde d'une commande",
            description = "Retourne le montant total, payé et le solde restant dû.")
    @ApiResponse(responseCode = "200", description = "Solde — ex: { \"total\": 20000, \"paye\": 10000, \"restant\": 10000 }")
    @GetMapping("/commandes/{id}/balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(
            @Parameter(description = "ID de la commande") @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getBalanceCommande(id));
    }

    @Operation(summary = "Annuler un paiement",
            description = "Annule et supprime un paiement enregistré. ADMIN uniquement.")
    @ApiResponse(responseCode = "204", description = "Paiement annulé")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelledPayment(
            @Parameter(description = "ID du paiement") @PathVariable Long id) {
        paymentService.cancelledPayment(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Vérifier si une commande est payée intégralement")
    @ApiResponse(responseCode = "200", description = "Résultat — ex: { \"verified\": true }")
    @GetMapping("/verification/{commandeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> verifyIfOrderPayed(
            @Parameter(description = "ID de la commande") @PathVariable Long commandeId) {
        Boolean result = paymentService.verifyIfOrderPayed(commandeId);
        return ResponseEntity.ok(Map.of("verified", result));
    }
}
