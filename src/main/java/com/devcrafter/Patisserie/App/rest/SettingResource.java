package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.dto.request.DeliveryZoneRequest;
import com.devcrafter.Patisserie.App.dto.request.SettingRequest;
import com.devcrafter.Patisserie.App.dto.request.TemplateMessageRequest;
import com.devcrafter.Patisserie.App.dto.response.DeliveryZoneResponse;
import com.devcrafter.Patisserie.App.dto.response.SettingResponse;
import com.devcrafter.Patisserie.App.dto.response.TemplateMessageResponse;
import com.devcrafter.Patisserie.App.services.SettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@Tag(name = "Paramètres", description = "Configuration générale, zones de livraison et templates de messages")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@SecurityRequirement(name = "BearerAuth")
public class SettingResource {

    private final SettingService settingService;

    @Operation(summary = "Récupérer les paramètres",
            description = "Retourne la configuration générale : nom pâtisserie, contact WhatsApp, délai de livraison min...")
    @ApiResponse(responseCode = "200", description = "Paramètres actuels",
            content = @Content(schema = @Schema(implementation = SettingResponse.class)))
    @GetMapping("/settings")
    public ResponseEntity<SettingResponse> getSettings() {
        return ResponseEntity.ok(settingService.getSettings());
    }

    @Operation(summary = "Mettre à jour les paramètres")
    @ApiResponse(responseCode = "200", description = "Paramètres mis à jour")
    @PutMapping("/settings")
    public ResponseEntity<SettingResponse> updateSettings(@RequestBody SettingRequest request) {
        return ResponseEntity.ok(settingService.updateSettings(request));
    }

    @Operation(summary = "Lister les zones de livraison")
    @ApiResponse(responseCode = "200", description = "Zones de livraison disponibles")
    @GetMapping("/delivery-zones")
    public ResponseEntity<List<DeliveryZoneResponse>> getZones() {
        return ResponseEntity.ok(settingService.getZones());
    }

    @Operation(summary = "Créer une zone de livraison")
    @ApiResponse(responseCode = "200", description = "Zone créée")
    @PostMapping("/delivery-zones")
    public ResponseEntity<DeliveryZoneResponse> createZone(@RequestBody DeliveryZoneRequest request) {
        return ResponseEntity.ok(settingService.createZone(request));
    }

    @Operation(summary = "Modifier une zone de livraison")
    @ApiResponse(responseCode = "200", description = "Zone modifiée")
    @PutMapping("/delivery-zones/{id}")
    public ResponseEntity<DeliveryZoneResponse> modifyZone(
            @Parameter(description = "ID de la zone") @PathVariable Long id,
            @RequestBody DeliveryZoneRequest request) {
        return ResponseEntity.ok(settingService.modifyZone(id, request));
    }

    @Operation(summary = "Supprimer une zone de livraison")
    @ApiResponse(responseCode = "204", description = "Zone supprimée")
    @DeleteMapping("/delivery-zones/{id}")
    public ResponseEntity<Void> deleteZone(
            @Parameter(description = "ID de la zone") @PathVariable Long id) {
        settingService.deleteZone(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lister les templates de messages",
            description = "Retourne les templates WhatsApp/email utilisés pour les notifications automatiques.")
    @ApiResponse(responseCode = "200", description = "Liste des templates")
    @GetMapping("/templates-messages")
    public ResponseEntity<List<TemplateMessageResponse>> getTemplates() {
        return ResponseEntity.ok(settingService.getTemplates());
    }

    @Operation(summary = "Modifier un template de message")
    @ApiResponse(responseCode = "200", description = "Template modifié")
    @PutMapping("/templates-messages/{id}")
    public ResponseEntity<TemplateMessageResponse> updateTemplate(
            @Parameter(description = "ID du template") @PathVariable Long id,
            @RequestBody TemplateMessageRequest request) {
        return ResponseEntity.ok(settingService.updateTemplate(id, request));
    }

    @Operation(summary = "Lister les villes de livraison",
            description = "Retourne la liste unique, triée par ordre alphabétique, de toutes les villes configurées dans les zones de livraison.")
    @ApiResponse(responseCode = "200", description = "Liste des villes récupérée")
    @GetMapping("/delivery-zones/cities")
    public ResponseEntity<List<String>> getVilles() {
        List<String> villes = settingService.getZones()
                .stream()
                .map(DeliveryZoneResponse::getName)
                .distinct()
                .sorted()
                .toList();
        return ResponseEntity.ok(villes);
    }

    @Operation(summary = "Lister les quartiers d'une ville",
            description = "Retourne toutes les zones de livraison (quartiers) correspondant à une ville donnée pour l'autocomplétion.")
    @ApiResponse(responseCode = "200", description = "Liste des quartiers récupérée")
    @GetMapping("/delivery-zones/neighborhood")
    public ResponseEntity<List<DeliveryZoneResponse>> getQuartiersForVille(
            @RequestParam String ville) {
        List<DeliveryZoneResponse> quartiers
                = settingService.getZones()
                        .stream()
                        .filter(z -> z.getName()
                                .equalsIgnoreCase(ville))
                        .toList();
        return ResponseEntity.ok(quartiers);
    }
}
