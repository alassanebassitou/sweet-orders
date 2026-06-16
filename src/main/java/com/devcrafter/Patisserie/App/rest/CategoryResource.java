package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.dto.request.CategoryRequest;
import com.devcrafter.Patisserie.App.dto.response.CategoryResponse;
import com.devcrafter.Patisserie.App.services.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


@Tag(name = "Catégories", description = "Gestion des catégories de produits")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
@SecurityRequirement(name = "BearerAuth")
public class CategoryResource {

    private final CategoryService categoryService;

    @Operation(summary = "Créer une catégorie")
    @ApiResponse(responseCode = "200", description = "Catégorie créée",
            content = @Content(schema = @Schema(implementation = CategoryResponse.class)))
    @PostMapping
    public ResponseEntity<CategoryResponse> createdCategory(
            @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.createdCategory(request));
    }

    @Operation(summary = "Récupérer une catégorie par ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catégorie trouvée"),
            @ApiResponse(responseCode = "404", description = "Catégorie introuvable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(
            @Parameter(description = "ID de la catégorie") @PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @Operation(summary = "Modifier une catégorie")
    @ApiResponse(responseCode = "200", description = "Catégorie modifiée")
    @PutMapping("/{catId}")
    public ResponseEntity<CategoryResponse> modifyCategory(
            @Parameter(description = "ID de la catégorie") @PathVariable Long catId,
            @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.modifyCategory(catId, request));
    }

    @Operation(summary = "Lister toutes les catégories")
    @ApiResponse(responseCode = "200", description = "Liste des catégories")
    @GetMapping("/all")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @Operation(summary = "Uploader la photo d'une catégorie",
            description = "Remplace ou définit la photo de la catégorie.")
    @ApiResponse(responseCode = "200", description = "Photo mise à jour")
    @PostMapping("/{id}/photo")
    public ResponseEntity<CategoryResponse> uploadPhoto(
            @Parameter(description = "ID de la catégorie") @PathVariable Long id,
            @Parameter(description = "Fichier image") @RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(categoryService.uploadPhoto(id, file));
    }

    @Operation(summary = "Supprimer une catégorie")
    @ApiResponse(responseCode = "204", description = "Catégorie supprimée")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "ID de la catégorie") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
