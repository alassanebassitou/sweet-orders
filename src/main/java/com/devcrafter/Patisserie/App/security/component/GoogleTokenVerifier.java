package com.devcrafter.Patisserie.App.security.component;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${app.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier
                .Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();

        log.info("GoogleTokenVerifier initialized with clientId: {}", clientId);
    }

    /**
     * Verifies the token signature with Google's public keys.
     * Returns the payload (email, name, sub, photo) or null if invalid.
     */
    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                log.error("Token verification returned null — possible causes:");
                log.error("  1. Token is expired (tokens expire after 1 hour)");
                log.error("  2. Client ID mismatch");
                log.error("  3. Token was not issued for this app");
                return null;
            }    // Invalid or expired token

            log.info("Token valid — email: {}", idToken.getPayload().getEmail());
            return idToken.getPayload();
        } catch (Exception e) {
            log.error("Token verification threw exception: {}", e.getMessage());
            log.error("Exception type: {}", e.getClass().getName());
            return null;
        }
    }

}
