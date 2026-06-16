package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.dto.request.EmailLoginRequest;
import com.devcrafter.Patisserie.App.dto.request.GoogleLoginRequest;
import com.devcrafter.Patisserie.App.dto.request.SignUpRequest;
import com.devcrafter.Patisserie.App.dto.request.VerifyCodeRequest;
import com.devcrafter.Patisserie.App.dto.response.AuthResponse;
import com.devcrafter.Patisserie.App.models.SessionUser;
import com.devcrafter.Patisserie.App.security.service.AuthService;
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

import java.util.Map;

import static com.devcrafter.Patisserie.App.utils.AppConstants.CURRENT_USER;
import static com.devcrafter.Patisserie.App.utils.AppConstants.SESSION_ID;

@Tag(name = "Authentification", description = "Login Google OAuth2, session courante et logout")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthResource {

    private final AuthService authService;

    @Operation(summary = "Connexion via Google",
            description = "Échange un idToken Google contre un token de session Sweet Orders.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie — retourne le token de session",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token Google invalide ou expiré")
    })
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "idToken obtenu depuis Google Sign-In", required = true)
            @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.loginWithGoogle(request.getIdToken());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Récupérer l'utilisateur connecté",
            description = "Retourne les informations de la session active.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session valide",
                    content = @Content(schema = @Schema(implementation = SessionUser.class))),
            @ApiResponse(responseCode = "401", description = "Session expirée ou absente")
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/me")
    public ResponseEntity<SessionUser> me(HttpServletRequest request) {
        SessionUser user = (SessionUser) request.getAttribute(CURRENT_USER);
        return ResponseEntity.ok(user);
    }


    @Operation(summary = "Créer un compte utilisateur",
            description = "Enregistre un nouvel utilisateur avec un email et un mot de passe.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données d'inscription invalides ou email déjà utilisé")
    })
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signUp(
            @Valid @RequestBody SignUpRequest request) {
        authService.signUp(request);
        return ResponseEntity.ok(Map.of(
                "message",
                "Compte créé avec succès. " +
                        "Connectez-vous avec votre email."
        ));
    }


    @Operation(summary = "Envoyer un code de vérification",
            description = "Génère et envoie un code de vérification par email pour l'authentification sans mot de passe.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Code de vérification envoyé avec succès"),
            @ApiResponse(responseCode = "400", description = "Format de l'adresse email invalide")
    })
    @PostMapping("/email/send-code")
    public ResponseEntity<Map<String, String>> sendCode(
            @Valid @RequestBody EmailLoginRequest request) {
        authService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok(Map.of(
                "message",
                "Code envoyé à " + request.getEmail()
        ));
    }


    @Operation(summary = "Vérifier le code et connecter",
            description = "Vérifie le code reçu par email. Si le code est valide, retourne un token de session Sweet Orders.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Code valide — Connexion réussie",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Code de vérification incorrect ou expiré")
    })
    @PostMapping("/email/verify")
    public ResponseEntity<AuthResponse> verifyCode(
            @Valid @RequestBody VerifyCodeRequest request) {
        return ResponseEntity.ok(
                authService.verifyCodeAndLogin(request)
        );
    }

    @Operation(summary = "Déconnexion", description = "Invalide la session courante.")
    @ApiResponse(responseCode = "204", description = "Déconnecté avec succès")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String sessionId = (String) request.getAttribute(SESSION_ID);
        authService.logout(sessionId);
        return ResponseEntity.noContent().build();
    }
}
