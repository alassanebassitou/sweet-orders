package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.dto.DailyPlanning;
import com.devcrafter.Patisserie.App.dto.DailyProductionForm;
import com.devcrafter.Patisserie.App.services.ProductionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@Tag(name = "Production", description = "Planning et feuilles de production")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/productions")
@SecurityRequirement(name = "BearerAuth")
public class ProductionResource {

    private final ProductionService productionService;

    @Operation(summary = "Planning hebdomadaire",
            description = "Retourne le planning de production pour la semaine débutant à `dateDebut`. Par défaut : lundi de la semaine courante.")
    @ApiResponse(responseCode = "200", description = "Planning de la semaine")
    @GetMapping("/planning")
    public ResponseEntity<List<DailyPlanning>> getPlanning(
            @Parameter(description = "Date de début de semaine (YYYY-MM-DD). Défaut : lundi courant.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut) {
        if (dateDebut == null) {
            dateDebut = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        }
        return ResponseEntity.ok(productionService.getPlanning(dateDebut));
    }

    @Operation(summary = "Feuille de production journalière",
            description = "Retourne la feuille de production pour une date donnée. Défaut : aujourd'hui.")
    @ApiResponse(responseCode = "200", description = "Feuille journalière")
    @GetMapping("/day")
    public ResponseEntity<DailyProductionForm> getDailyForm(
            @Parameter(description = "Date (YYYY-MM-DD). Défaut : aujourd'hui.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        return ResponseEntity.ok(productionService.getDailyForm(date));
    }

    @Operation(summary = "Marquer une commande comme produite",
            description = "Indique que la production de la commande est terminée.")
    @ApiResponse(responseCode = "204", description = "Commande marquée comme produite")
    @PatchMapping("/{commandeId}/finish")
    public ResponseEntity<Void> markFinished(
            @Parameter(description = "ID de la commande") @PathVariable Long commandeId) {
        productionService.markFinished(commandeId);
        return ResponseEntity.noContent().build();
    }
}
