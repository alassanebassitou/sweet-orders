package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.dto.response.ProductCustomizationResponse;
import com.devcrafter.Patisserie.App.dto.response.ProductResponse;
import com.devcrafter.Patisserie.App.services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "Produits", description = "Catalogue produits public — consultation et personnalisations")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductsResource {

    private final ProductService productService;

    @Operation(summary = "Tous les produits actifs",
            description = "Retourne le catalogue complet des produits visibles. Résultat mis en cache Redis.")
    @ApiResponse(responseCode = "200", description = "Liste des produits",
            content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @Operation(summary = "Détail d'un produit")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produit trouvé"),
            @ApiResponse(responseCode = "404", description = "Produit introuvable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(
            @Parameter(description = "ID du produit") @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @Operation(summary = "Personnalisations d'un produit",
            description = "Retourne les options de personnalisation disponibles pour un produit (taille, saveur, décoration...).")
    @ApiResponse(responseCode = "200", description = "Options de personnalisation")
    @GetMapping("/{id}/customization")
    public ResponseEntity<List<ProductCustomizationResponse>> getCustomization(
            @Parameter(description = "ID du produit") @PathVariable Long id) {
        return ResponseEntity.ok(productService.getCustomization(id));
    }
}
