package com.devcrafter.Patisserie.App.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.swagger.api-url:http://localhost:8080}")
    private String apiUrl;

    @Bean
    public OpenAPI sweetOrdersOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sweet Orders API")
                        .description("""
                                API de gestion de pâtisserie — Sweet Orders.
                                
                                ## Authentification
                                Toutes les routes protégées nécessitent le header **`X-Session-Id`**.
                                
                                ### Comment obtenir un sessionId ?
                                1. Appeler `POST /api/v1/auth/google` avec un `idToken` Google
                                2. La réponse contient `{ "sessionId": "...", "sessionUser": { ... } }`
                                3. Copier le `sessionId` et le coller dans le champ **X-Session-Id** ci-dessous
                                
                                ### Durée de session
                                La session expire après **8 heures** d'inactivité.
                                Elle est automatiquement rafraîchie à chaque requête.
                                
                                ### Rôles
                                - **ADMIN** : accès complet (produits, commandes, finances, utilisateurs, livraisons)
                                - **CLIENT** : accès à son propre profil, commandes, paiements et avis
                                
                                ### Codes d'erreur d'authentification
                                - `401` — Header `X-Session-Id` absent ou session expirée
                                - `403` — Compte désactivé par l'administrateur
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Sweet Orders")
                                .email("contact@sweetorders.com")))
                .servers(List.of(
                        new Server()
                                .url(apiUrl)
                                .description("Current development environment")))

                .addSecurityItem(new SecurityRequirement().addList("SessionAuth"))

                .components(new Components()
                        .addSecuritySchemes("SessionAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Session-Id")
                                .description("""
                                        Session ID obtenu après `POST /api/v1/auth/google`.
                                        Valide pendant 8h, rafraîchi automatiquement à chaque requête.
                                        """)));
    }
}
