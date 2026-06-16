package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.dto.request.ExpenseRequest;
import com.devcrafter.Patisserie.App.dto.response.ExpenseResponse;
import com.devcrafter.Patisserie.App.dto.response.FinanceDashboardResponse;
import com.devcrafter.Patisserie.App.enums.ExpensesCategory;
import com.devcrafter.Patisserie.App.services.FinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@Tag(name = "Finances", description = "Tableau de bord financier, dépenses et export CSV (ADMIN uniquement)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class FinanceResource {

    private final FinanceService financeService;

    @Operation(summary = "Tableau de bord financier",
            description = "Retourne les KPIs financiers : revenus, dépenses, solde, commandes en cours.")
    @ApiResponse(responseCode = "200", description = "Dashboard financier",
            content = @Content(schema = @Schema(implementation = FinanceDashboardResponse.class)))
    @GetMapping("/finances/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FinanceDashboardResponse> getDashboard() {
        return ResponseEntity.ok(financeService.getDashboard());
    }

    @Operation(summary = "Créer une dépense")
    @ApiResponse(responseCode = "200", description = "Dépense créée",
            content = @Content(schema = @Schema(implementation = ExpenseResponse.class)))
    @PostMapping("/expenses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExpenseResponse> createExpense(@RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(financeService.createExpense(request));
    }

    @Operation(summary = "Lister les dépenses",
            description = "Retourne les dépenses filtrées par catégorie et/ou période.")
    @ApiResponse(responseCode = "200", description = "Liste des dépenses")
    @GetMapping("/expenses")
    public ResponseEntity<List<ExpenseResponse>> getExpenses(
            @Parameter(description = "Catégorie (optionnelle)") @RequestParam(required = false) ExpensesCategory category,
            @Parameter(description = "Date de début (YYYY-MM-DD)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "Date de fin (YYYY-MM-DD)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(financeService.getExpenses(category, start, end));
    }

    @Operation(summary = "Modifier une dépense")
    @ApiResponse(responseCode = "200", description = "Dépense modifiée")
    @PutMapping("/expenses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExpenseResponse> modifyExpense(
            @Parameter(description = "ID de la dépense") @PathVariable Long id,
            @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(financeService.modifyExpense(id, request));
    }

    @Operation(summary = "Supprimer une dépense")
    @ApiResponse(responseCode = "204", description = "Dépense supprimée")
    @DeleteMapping("/expenses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteExpenses(
            @Parameter(description = "ID de la dépense") @PathVariable Long id) {
        financeService.deleteExpenses(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Exporter les finances en CSV",
            description = "Génère un fichier CSV de toutes les transactions sur la période donnée.")
    @ApiResponse(responseCode = "200", description = "Fichier CSV — Content-Disposition: attachment; filename=finances.csv")
    @GetMapping("/finances/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportCsv(
            @Parameter(description = "Date de début") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "Date de fin") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        String csv = financeService.exportCsv(start, end);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"finances.csv\"")
                .header("Content-Type", "text/csv; charset=UTF-8")
                .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
