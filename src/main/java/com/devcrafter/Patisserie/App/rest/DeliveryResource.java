package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.dto.request.DeliveryRequest;
import com.devcrafter.Patisserie.App.dto.request.FailureRequest;
import com.devcrafter.Patisserie.App.dto.request.ReprogramRequest;
import com.devcrafter.Patisserie.App.dto.response.DeliveryResponse;
import com.devcrafter.Patisserie.App.enums.DeliveryStatus;
import com.devcrafter.Patisserie.App.models.SessionUser;
import com.devcrafter.Patisserie.App.services.DeliveryService;
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

import java.util.List;

import static com.devcrafter.Patisserie.App.utils.AppConstants.CURRENT_USER;


@Tag(name = "Livraisons", description = "Planification et suivi des livraisons (ADMIN uniquement)")
@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class DeliveryResource {

    private final DeliveryService deliveryService;

    @Operation(summary = "Planifier une livraison")
    @ApiResponse(responseCode = "200", description = "Livraison planifiée",
            content = @Content(schema = @Schema(implementation = DeliveryResponse.class)))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryResponse> planifier(
            @RequestBody DeliveryRequest request,
            HttpServletRequest httpRequest) {
        SessionUser user = (SessionUser) httpRequest.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(deliveryService.planifierLivraison(request, user));
    }

    @Operation(summary = "Tournée du jour",
            description = "Retourne toutes les livraisons prévues pour aujourd'hui.")
    @ApiResponse(responseCode = "200", description = "Livraisons du jour")
    @GetMapping("/today")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryResponse>> getDailyShot() {
        return ResponseEntity.ok(deliveryService.getDailyShot());
    }

    @Operation(summary = "Lister les livraisons",
            description = "Retourne toutes les livraisons avec filtre optionnel par statut.")
    @ApiResponse(responseCode = "200", description = "Liste des livraisons")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryResponse>> getDelivery(
            @Parameter(description = "Filtrer par statut (optionnel)", example = "PLANIFIEE")
            @RequestParam(required = false) DeliveryStatus statut) {
        return ResponseEntity.ok(deliveryService.getDelivery(statut));
    }

    @Operation(summary = "Vue calendrier des livraisons",
            description = "Retourne toutes les livraisons d'un mois donné.")
    @ApiResponse(responseCode = "200", description = "Livraisons du mois")
    @GetMapping("/calendar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryResponse>> getCalendar(
            @Parameter(description = "Mois (1-12)", example = "6") @RequestParam int month,
            @Parameter(description = "Année", example = "2026") @RequestParam int year) {
        return ResponseEntity.ok(deliveryService.getCalendar(month, year));
    }

    @Operation(summary = "Marquer une livraison comme effectuée")
    @ApiResponse(responseCode = "200", description = "Livraison confirmée")
    @PostMapping("/{id}/delivered")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryResponse> markDelivered(
            @Parameter(description = "ID de la livraison") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        SessionUser user = (SessionUser) httpRequest.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(deliveryService.markDelivered(id, user));
    }

    @Operation(summary = "Signaler un échec de livraison")
    @ApiResponse(responseCode = "200", description = "Échec enregistré")
    @PostMapping("/{id}/failure")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryResponse> reportedFailure(
            @Parameter(description = "ID de la livraison") @PathVariable Long id,
            @RequestBody FailureRequest request,
            HttpServletRequest httpRequest) {
        SessionUser user = (SessionUser) httpRequest.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(deliveryService.reportedFailure(id, request, user));
    }

    @Operation(summary = "Reprogrammer une livraison")
    @ApiResponse(responseCode = "200", description = "Livraison reprogrammée")
    @PutMapping("/{id}/reprogrammed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryResponse> reprogramme(
            @Parameter(description = "ID de la livraison") @PathVariable Long id,
            @RequestBody ReprogramRequest request,
            HttpServletRequest httpRequest) {
        SessionUser user = (SessionUser) httpRequest.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(deliveryService.reprogramme(id, request, user));
    }
}
