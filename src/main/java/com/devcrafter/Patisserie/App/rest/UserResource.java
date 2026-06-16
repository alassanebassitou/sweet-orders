package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.dto.request.PhoneRequest;
import com.devcrafter.Patisserie.App.dto.request.UserProfileRequest;
import com.devcrafter.Patisserie.App.dto.response.UserResponse;
import com.devcrafter.Patisserie.App.models.SessionUser;
import com.devcrafter.Patisserie.App.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.*;

import static com.devcrafter.Patisserie.App.utils.AppConstants.CURRENT_USER;

@Tag(name = "Utilisateurs", description = "Gestion du profil utilisateur connecté")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "BearerAuth")
public class UserResource {

    private final UserService userService;

    @Operation(summary = "Mettre à jour mon profil",
            description = "Permet à l'utilisateur connecté de modifier son nom, prénom, téléphone, et adresse.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil mis à jour",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @RequestBody UserProfileRequest request,
            HttpServletRequest httpRequest) {
        SessionUser currentUser = (SessionUser) httpRequest.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(userService.updateMyProfile(currentUser, request));
    }


    @Operation(summary = "Mettre à jour le numéro de téléphone",
            description = "Permet à l'utilisateur connecté de modifier ou d'associer un numéro de téléphone à son profil.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Numéro de téléphone mis à jour",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Numéro de téléphone invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @PatchMapping("/users/me/phone")
    public ResponseEntity<UserResponse> updatePhone(
            @RequestBody @Valid PhoneRequest request,
            HttpServletRequest httpRequest) {

        SessionUser user = (SessionUser)
                httpRequest.getAttribute("currentUser");
        return ResponseEntity.ok(
                userService.updatePhone(
                        user.getUserId(), request.getTelephone()
                )
        );
    }

}
