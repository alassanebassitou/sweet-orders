package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.dto.request.*;
import com.devcrafter.Patisserie.App.dto.response.*;
import com.devcrafter.Patisserie.App.enums.CommandStatus;
import com.devcrafter.Patisserie.App.models.SessionUser;
import com.devcrafter.Patisserie.App.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static com.devcrafter.Patisserie.App.utils.AppConstants.CURRENT_USER;


@Tag(name = "Admin — Utilisateurs", description = "Gestion des utilisateurs (ADMIN uniquement)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "BearerAuth")
public class AdminResource {

    private final UserService userService;
    private final ProductService productService;
    private final CommandeService commandeService;
    private final FinanceService financeService;
    private final ReviewsService reviewsService;
    private final PaymentService paymentService;

    // ─────────────────────────────────────────────────────────────
    // UTILISATEURS
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "Créer un client manuellement",
            description = "Crée un compte client sans passer par Google OAuth. ADMIN uniquement.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client créé avec succès",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis")
    })
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createdClient(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Informations du client à créer", required = true)
            @RequestBody ClientManuallyCreationRequest request) {
        return ResponseEntity.status(201).body(userService.createdClientManually(request));
    }

    @Operation(summary = "Lister tous les utilisateurs",
            description = "Retourne la liste complète des utilisateurs. ADMIN uniquement.")
    @ApiResponse(responseCode = "200", description = "Liste des utilisateurs",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Changer le rôle d'un utilisateur",
            description = "Modifie le rôle d'un utilisateur (ex: CLIENT → ADMIN). ADMIN uniquement.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rôle modifié"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeUserRole(
            @Parameter(description = "ID de l'utilisateur", required = true) @PathVariable Long id,
            @RequestBody UserRoleRequest request,
            HttpServletRequest httpRequest) {
        SessionUser currentUser = (SessionUser) httpRequest.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(userService.changeUserRole(id, request, currentUser));
    }

    @Operation(summary = "Désactiver un utilisateur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilisateur désactivé"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @DeleteMapping("/users/{id}")
    public ResponseEntity<UserResponse> deactivateUser(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long id,
            HttpServletRequest request) {
        SessionUser currentUser = (SessionUser) request.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(userService.deactivateUser(id, currentUser));
    }

    @Operation(summary = "Réactiver un utilisateur")
    @ApiResponse(responseCode = "200", description = "Utilisateur réactivé")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long id) {
        return ResponseEntity.ok(userService.activateUser(id));
    }

    // ─────────────────────────────────────────────────────────────
    // PRODUITS
    // ─────────────────────────────────────────────────────────────

    @Tag(name = "Admin — Produits")
    @Operation(summary = "Créer un produit")
    @ApiResponse(responseCode = "200", description = "Produit créé",
            content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createdProduit(@RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @Operation(summary = "Modifier un produit")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produit modifié"),
            @ApiResponse(responseCode = "404", description = "Produit introuvable")
    })
    @PutMapping("/products/{id}")
    public ResponseEntity<ProductResponse> modifierProduit(
            @Parameter(description = "ID du produit") @PathVariable Long id,
            @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.modifierProduit(id, request));
    }

    @Operation(summary = "Désactiver un produit",
            description = "Désactive le produit (suppression logique — il n'apparaît plus dans le catalogue).")
    @ApiResponse(responseCode = "204", description = "Produit désactivé")
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deactivateProduit(
            @Parameter(description = "ID du produit") @PathVariable Long id) {
        productService.deactivatedProduct(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Uploader une photo supplémentaire",
            description = "Ajoute une photo supplémentaire à la galerie du produit.")
    @ApiResponse(responseCode = "200", description = "Photo ajoutée")
    @PostMapping("/products/{id}/pictures")
    public ResponseEntity<ProductResponse> uploadPhoto(
            @Parameter(description = "ID du produit") @PathVariable Long id,
            @Parameter(description = "Fichier image") @RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(productService.uploadPhoto(id, file));
    }

    @Operation(summary = "Ajouter une personnalisation au produit")
    @ApiResponse(responseCode = "200", description = "Personnalisation ajoutée")
    @PostMapping("/products/{id}/customization")
    public ResponseEntity<ProductCustomizationResponse> addCustomization(
            @Parameter(description = "ID du produit") @PathVariable Long id,
            @RequestBody ProductCustomizationRequest request) {
        return ResponseEntity.ok(productService.addCustomization(id, request));
    }

    @Operation(summary = "Mettre à jour une option de personnalisation",
            description = "Modifie une option de personnalisation existante d'un produit. ADMIN uniquement.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Option de personnalisation mise à jour",
                    content = @Content(schema = @Schema(implementation = ProductCustomizationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Option de personnalisation introuvable")
    })
    @PutMapping("/produits/customizations/{id}")
    public ResponseEntity<ProductCustomizationResponse>
    updateCustomization(
            @PathVariable Long id,
            @RequestBody ProductCustomizationRequest request) {

        return ResponseEntity.ok(productService.updateCustomization(id, request));
    }

    @Operation(summary = "Supprimer une option de personnalisation",
            description = "Supprime définitivement une option de personnalisation d'un produit. ADMIN uniquement.")
    @ApiResponse(responseCode = "204", description = "Option de personnalisation supprimée")
    @DeleteMapping("/produits/customizations/{id}")
    public ResponseEntity<Void> deleteCustomization(
            @PathVariable Long id) {
        productService.deleteCustomization(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remplacer la photo principale du produit")
    @ApiResponse(responseCode = "200", description = "Photo principale mise à jour")
    @PostMapping("/products/{id}/photo")
    public ResponseEntity<ProductResponse> uploadMainPhoto(
            @Parameter(description = "ID du produit") @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(productService.uploadMainPhoto(id, file));
    }

    @Operation(summary = "Ajouter une photo additionnelle")
    @ApiResponse(responseCode = "200", description = "Photo additionnelle ajoutée")
    @PostMapping("/products/{id}/photos")
    public ResponseEntity<ProductResponse> addPhoto(
            @Parameter(description = "ID du produit") @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(productService.addAdditionalPhoto(id, file));
    }

    @Operation(summary = "Supprimer une photo additionnelle")
    @ApiResponse(responseCode = "200", description = "Photo supprimée")
    @DeleteMapping("/products/{id}/photos")
    public ResponseEntity<ProductResponse> deletePhoto(
            @Parameter(description = "ID du produit") @PathVariable Long id,
            @Parameter(description = "URL de la photo à supprimer") @RequestParam("url") String photoUrl) {
        return ResponseEntity.ok(productService.deleteAdditionalPhoto(id, photoUrl));
    }

    // ─────────────────────────────────────────────────────────────
    // COMMANDES
    // ─────────────────────────────────────────────────────────────

    @Tag(name = "Admin — Commandes")
    @Operation(summary = "Lister toutes les commandes",
            description = "Retourne toutes les commandes, avec filtre optionnel par statut.")
    @ApiResponse(responseCode = "200", description = "Liste des commandes")
    @GetMapping("/commandes")
    public ResponseEntity<List<CommandeResponse>> getAllCommandes(
            @Parameter(description = "Filtrer par statut (optionnel)", example = "EN_ATTENTE")
            @RequestParam(required = false) CommandStatus status) {
        return ResponseEntity.ok(commandeService.getAllCommandes(status));
    }

    @Operation(summary = "Changer le statut d'une commande")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut modifié"),
            @ApiResponse(responseCode = "404", description = "Commande introuvable")
    })
    @PatchMapping("/commandes/{id}/status")
    public ResponseEntity<CommandeResponse> changerStatut(
            @Parameter(description = "ID de la commande") @PathVariable Long id,
            @RequestBody StatusRequest request,
            HttpServletRequest httpRequest) {
        SessionUser user = (SessionUser) httpRequest.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(commandeService.changerStatut(id, request, user));
    }

    @Operation(summary = "Dépenses liées à une commande")
    @ApiResponse(responseCode = "200", description = "Liste des dépenses")
    @GetMapping("/commandes/{id}/expenses")
    public ResponseEntity<List<ExpenseResponse>> getExpensesCommande(
            @Parameter(description = "ID de la commande") @PathVariable Long id) {
        return ResponseEntity.ok(financeService.getExpensesByCommande(id));
    }


    @Operation(summary = "Appliquer des frais de livraison",
            description = "Permet à un administrateur d'affecter ou de modifier manuellement les frais de livraison d'une commande spécifique. ADMIN uniquement.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Frais de livraison appliqués avec succès",
                    content = @Content(schema = @Schema(implementation = CommandeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de la requête invalides"),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis"),
            @ApiResponse(responseCode = "404", description = "Commande introuvable")
    })
    @PutMapping("/commandes/{id}/delivery-fees")
    public ResponseEntity<CommandeResponse> applyDeliveryFee(
            @PathVariable Long id,
            @RequestBody @Valid ApplyDeliveryFeeRequest request,
            HttpServletRequest httpRequest) {

        SessionUser admin = (SessionUser)
                httpRequest.getAttribute("currentUser");

        return ResponseEntity.ok(
                commandeService.applyDeliveryFee(id, request, admin)
        );
    }

    @Operation(summary = "Supprimer plusieurs commandes en masse",
            description = "Supprime définitivement un lot de commandes spécifiées dans la requête. ADMIN uniquement.")
    @ApiResponse(responseCode = "204", description = "Commandes supprimées avec succès")
    @DeleteMapping("/commandes/bulk")
    public ResponseEntity<Void> deleteCommandes(@RequestBody DeleteCommandeRequest request) {
        commandeService.deleteCommandes(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Supprimer définitivement une commande",
            description = "Supprime une seule commande spécifique à partir de son ID. ADMIN uniquement.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Commande supprimée avec succès"),
            @ApiResponse(responseCode = "404", description = "Commande introuvable")
    })
    @DeleteMapping("/commandes/{id}")
    public ResponseEntity<Void> deleteOneCommande(@PathVariable("id") Long id) {
        commandeService.deleteOneCommande(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────
    // AVIS
    // ─────────────────────────────────────────────────────────────

    @Tag(name = "Admin — Avis")
    @Operation(summary = "Lister tous les avis")
    @ApiResponse(responseCode = "200", description = "Liste des avis")
    @GetMapping("/reviews")
    public ResponseEntity<List<ReviewsResponse>> getAllReviews() {
        return ResponseEntity.ok(reviewsService.getAllReviews());
    }

    @Operation(summary = "Masquer un avis",
            description = "Cache l'avis du catalogue public sans le supprimer définitivement.")
    @ApiResponse(responseCode = "204", description = "Avis masqué")
    @PatchMapping("/reviews/{id}/hidde")
    public ResponseEntity<Void> hiddeReviews(
            @Parameter(description = "ID de l'avis") @PathVariable Long id) {
        reviewsService.hiddeReview(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Restaurer un avis masqué")
    @ApiResponse(responseCode = "204", description = "Avis restauré")
    @PatchMapping("/reviews/{id}/restaure")
    public ResponseEntity<Void> restaureReview(
            @Parameter(description = "ID de l'avis") @PathVariable Long id) {
        reviewsService.restaureReviews(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Supprimer définitivement un avis")
    @ApiResponse(responseCode = "204", description = "Avis supprimé")
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> supprimerAvis(
            @Parameter(description = "ID de l'avis") @PathVariable Long id) {
        reviewsService.removeReviews(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Envoyer un rappel d'acompte par email",
            description = "Déclenche manuellement l'envoi d'un email de rappel pour le paiement de l'acompte.")
    @ApiResponse(responseCode = "204", description = "Email envoyé")
    @PostMapping("/payments/{commandeId}/relance-email")
    public ResponseEntity<Void> sendAcompteReminder(
            @Parameter(description = "ID de la commande") @PathVariable Long commandeId) {
        paymentService.sendAcompteReminder(commandeId);
        return ResponseEntity.noContent().build();
    }
}
