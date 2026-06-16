package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.dto.response.ReviewsStatsResponse;
import com.devcrafter.Patisserie.App.dto.request.ReviewsRequest;
import com.devcrafter.Patisserie.App.dto.response.ReviewsResponse;
import com.devcrafter.Patisserie.App.models.SessionUser;
import com.devcrafter.Patisserie.App.services.ReviewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.devcrafter.Patisserie.App.utils.AppConstants.CURRENT_USER;


@Tag(name = "Avis", description = "Soumission et consultation des avis clients")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewsResource {

    private final ReviewsService reviewsService;

    @Operation(summary = "Soumettre un avis",
            description = "Permet à un client connecté de soumettre un avis sur un produit commandé.")
    @ApiResponse(responseCode = "200", description = "Avis enregistré",
            content = @Content(schema = @Schema(implementation = ReviewsResponse.class)))
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/reviews")
    public ResponseEntity<ReviewsResponse> makeReview(
            @RequestBody ReviewsRequest request,
            HttpServletRequest httpRequest) {
        SessionUser user = (SessionUser) httpRequest.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(reviewsService.makeReview(request, user));
    }

    @Operation(summary = "Avis et statistiques d'un produit",
            description = "Retourne les avis visibles + note moyenne + répartition par étoiles pour un produit.")
    @ApiResponse(responseCode = "200", description = "Avis et stats",
            content = @Content(schema = @Schema(implementation = ReviewsStatsResponse.class)))
    @GetMapping("/products/{id}/reviews")
    public ResponseEntity<ReviewsStatsResponse> getProductReviews(
            @Parameter(description = "ID du produit") @PathVariable Long id) {
        return ResponseEntity.ok(reviewsService.getProductReviews(id));
    }

    @Operation(summary = "Mes avis",
            description = "Retourne tous les avis soumis par l'utilisateur connecté.")
    @ApiResponse(responseCode = "200", description = "Mes avis")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/reviews/my-avis")
    public ResponseEntity<List<ReviewsResponse>> getMyReviews(HttpServletRequest httpRequest) {
        SessionUser user = (SessionUser) httpRequest.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(reviewsService.getMyReviews(user));
    }
}
