package com.devcrafter.Patisserie.App.security.config;

import com.devcrafter.Patisserie.App.models.SessionUser;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorProvider")
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("SYSTEM");
        }

        Object principal = authentication.getPrincipal();

        // Case 1: UserDetails
        if (principal instanceof UserDetails userDetails) {
            return Optional.of(userDetails.getUsername());
        }

        // Case 2: JWT (OAuth2 / Kinde / Keycloak)
        /*if (principal instanceof Jwt jwt) {
            return Optional.of(jwt.getClaimAsString("email"));
        }*/

        // Case 3: OAuthsUser (GOOGLE / GIT / etc)
        if(principal instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            return Optional.of(
                    Optional.ofNullable(email)
                            .orElseGet(oAuth2User::getName)
            );
        }

        // Case 4 — Custom SessionUser
        // Set by SessionFilter via UsernamePasswordAuthenticationToken
        if (principal instanceof SessionUser sessionUser) {
            return Optional.of(sessionUser.getEmail());
        }

        // Case 5 — Anonymous user
        if ("anonymousUser".equals(principal)) {
            return Optional.of("ANONYMOUS");
        }

        return Optional.of("UNKNOWN");
    }
}
