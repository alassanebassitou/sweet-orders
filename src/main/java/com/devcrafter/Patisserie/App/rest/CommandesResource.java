package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.dto.request.CommandeRequest;
import com.devcrafter.Patisserie.App.dto.request.DuplicateRequest;
import com.devcrafter.Patisserie.App.dto.response.CommandeBalanceResponse;
import com.devcrafter.Patisserie.App.dto.response.CommandeResponse;
import com.devcrafter.Patisserie.App.models.SessionUser;
import com.devcrafter.Patisserie.App.services.CommandeService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.devcrafter.Patisserie.App.utils.AppConstants.CURRENT_USER;


@Tag(name = "Commandes", description = "Création et consultation des commandes client")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/commandes")
@SecurityRequirement(name = "BearerAuth")
public class CommandesResource {

    private final CommandeService commandeService;

    @Operation(summary = "Créer une commande",
            description = "Crée une nouvelle commande pour l'utilisateur connecté.")
    @ApiResponse(responseCode = "200", description = "Commande créée",
            content = @Content(schema = @Schema(implementation = CommandeResponse.class)))
    @PostMapping
    public ResponseEntity<CommandeResponse> createdCommande(
            @RequestBody CommandeRequest request,
            HttpServletRequest httpRequest) {
        SessionUser user = (SessionUser) httpRequest.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(commandeService.createdCommande(request, user));
    }

    @Operation(summary = "Mes commandes",
            description = "Retourne toutes les commandes de l'utilisateur connecté.")
    @ApiResponse(responseCode = "200", description = "Liste des commandes")
    @GetMapping("/my-commandes")
    public ResponseEntity<List<CommandeResponse>> getMesCommandes(HttpServletRequest request) {
        SessionUser user = (SessionUser) request.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(commandeService.getMyCommandes(user));
    }

    @Operation(summary = "Récupérer une commande par ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commande trouvée"),
            @ApiResponse(responseCode = "403", description = "Accès refusé — commande d'un autre client"),
            @ApiResponse(responseCode = "404", description = "Commande introuvable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CommandeResponse> getCommande(
            @Parameter(description = "ID de la commande") @PathVariable Long id,
            HttpServletRequest request) {
        SessionUser user = (SessionUser) request.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(commandeService.getCommande(id, user));
    }

    @Operation(summary = "Dupliquer une commande",
            description = "Crée une nouvelle commande identique avec une nouvelle date de livraison souhaitée.")
    @ApiResponse(responseCode = "200", description = "Commande dupliquée")
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<CommandeResponse> dupliquer(
            @Parameter(description = "ID de la commande à dupliquer") @PathVariable Long id,
            @RequestBody(required = false) DuplicateRequest duplicateRequest,
            HttpServletRequest request) {
        SessionUser user = (SessionUser) request.getAttribute(CURRENT_USER);
        LocalDate date = duplicateRequest.getWishDeliveryDate() != null
                ? duplicateRequest.getWishDeliveryDate()
                : LocalDate.now().plusDays(1);
        return ResponseEntity.ok(commandeService.dupliquerCommande(id, date, user));
    }

    @Operation(summary = "Solde d'une commande",
            description = "Retourne le montant total, les paiements effectués et le solde restant.")
    @ApiResponse(responseCode = "200", description = "Solde calculé",
            content = @Content(schema = @Schema(implementation = CommandeBalanceResponse.class)))
    @GetMapping("/{id}/balance")
    public ResponseEntity<CommandeBalanceResponse> getBalance(
            @Parameter(description = "ID de la commande") @PathVariable Long id) {
        return ResponseEntity.ok(commandeService.balance(id));
    }
}
