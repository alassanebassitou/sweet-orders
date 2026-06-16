package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.Exceptions.KkiapayPaymentException;
import com.devcrafter.Patisserie.App.config.KkiapayConfig;
import com.devcrafter.Patisserie.App.dto.response.PaymentResponse;
import com.devcrafter.Patisserie.App.models.SessionUser;
import com.devcrafter.Patisserie.App.services.PaymentWithKkiapayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.devcrafter.Patisserie.App.utils.AppConstants.CURRENT_USER;


@Tag(name = "Paiements — Kkiapay", description = "Paiement Mobile Money via Kkiapay")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Slf4j
public class PaymentWithKkiapayResource {

    private final PaymentWithKkiapayService paymentService;
    private final KkiapayConfig kkiapayConfig;

    @Operation(summary = "Configuration Kkiapay",
            description = "Retourne la clé publique et le mode sandbox pour initialiser le widget côté frontend.")
    @ApiResponse(responseCode = "200", description = "Config Kkiapay — ex: { \"publicKey\": \"...\", \"sandbox\": true }")
    @GetMapping("/payments/kkiapay/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
                "publicKey", kkiapayConfig.getPublicKey(),
                "sandbox", kkiapayConfig.isSandbox()));
    }

    @Operation(summary = "Vérifier et enregistrer un paiement Kkiapay",
            description = """
                       Appelé par le frontend après succès du widget Kkiapay.
                       Vérifie la transaction auprès de l'API Kkiapay puis l'enregistre en base.
                       
                       **Codes de retour d'erreur possibles dans le body :**
                       - `insufficient_fund` — solde Mobile Money insuffisant
                       - `declined` — paiement refusé par l'opérateur
                       - `error` — erreur technique
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paiement vérifié et enregistré",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "402", description = "Transaction invalide — body contient frontendStatus")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/payments/kkiapay/verify")
    public ResponseEntity<?> verifyAndRecord(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "transactionId et commandeId", required = true)
            @RequestBody Map<String, Object> body,
            HttpServletRequest httpRequest) {
        SessionUser user = (SessionUser) httpRequest.getAttribute(CURRENT_USER);
        String transactionId = (String) body.get("transactionId");
        Long commandeId = Long.parseLong(body.get("commandeId").toString());
        String type = (String) body.getOrDefault("type", "ACOMPTE");

        if ("DELIVERY_FEES".equals(type)) {
            return ResponseEntity.ok(
                    paymentService.verifyAndRecordDeliveryFees(
                            transactionId, commandeId, user
                    )
            );
        }
        try {
            return ResponseEntity.ok(paymentService
                    .verifyAndRecordKkiapay(transactionId, commandeId, user, type)
            );
        } catch (KkiapayPaymentException exception) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of(
                            "error", true,
                            "kkiapayStatus", exception.getKkiapayStatus(),
                            "frontendStatus", exception.getFrontendStatus(),
                            "message", exception.getMessage()));
        }
    }

    @Operation(summary = "Webhook Kkiapay",
            description = """
                       Endpoint PUBLIC appelé par les serveurs Kkiapay pour confirmer les paiements.
                       Vérifie la signature HMAC via le header `x-kkiapay-secret`.
                       Retourne toujours 200 pour éviter les rejeux Kkiapay.
                       """)
    @ApiResponse(responseCode = "200", description = "Webhook traité (toujours 200)")
    @PostMapping("/webhooks/kkiapay")
    public ResponseEntity<Void> kkiapayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-kkiapay-secret", required = false)
            @Parameter(description = "Signature HMAC Kkiapay") String signature) {
        try {
            paymentService.processKkiapayWebhook(payload, signature);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Webhook error: {}", e.getMessage());
            return ResponseEntity.ok().build();
        }
    }
}
