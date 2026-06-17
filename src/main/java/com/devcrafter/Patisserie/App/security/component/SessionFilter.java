package com.devcrafter.Patisserie.App.security.component;

import com.devcrafter.Patisserie.App.models.SessionUser;
import com.devcrafter.Patisserie.App.security.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static com.devcrafter.Patisserie.App.utils.AppConstants.CURRENT_USER;
import static com.devcrafter.Patisserie.App.utils.AppConstants.SESSION_ID;

@Component
@Slf4j
public class SessionFilter extends OncePerRequestFilter {

    private final SessionService sessionService;

    @Value("${app.session.header-name:X-Session-Id}")
    private String headerName;

    public SessionFilter(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        log.info("Request path: {}", path);

        // Skip auth endpoints — they don't need a session yet
        if (isPublicPath(request)) {
            chain.doFilter(request, response);
            return;
        }

        String sessionId = request.getHeader(headerName);

        if (sessionId == null || sessionId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Missing session header\"}");
            return;
        }

        SessionUser user = sessionService.getSession(sessionId);
        log.info("Header name looking for: {}", headerName);
        log.info("Session ID from header: {}", sessionId);

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Session expired or invalid\"}");
            return;
        }

        log.info("User found: {}", user.getEmail());
        log.info("User isActif: {}", user.getIsActif());
        log.info("User role: {}", user.getRole());

        // Check actif on every request
        // If admin deactivates a user, they are blocked on the very next request
        if (Boolean.FALSE.equals(user.getIsActif())) {
            // Delete their session immediately
            sessionService.deleteSession(sessionId);
            log.warn("No session header found");
            sendError(response, HttpServletResponse.SC_FORBIDDEN,
                    "Votre compte a été désactivé.");
            return;
        }

        UsernamePasswordAuthenticationToken authentication
                = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority(user.getRole()))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("Auth principal: {}",
                authentication.getPrincipal().toString());
        log.info("Auth authorities: {}",
                SecurityContextHolder.getContext().getAuthentication().getAuthorities());

        // Attach user to request so controllers can access it
        request.setAttribute(CURRENT_USER, user);
        request.setAttribute(SESSION_ID,  sessionId);

        // Refresh TTL — keeps active users logged in
        sessionService.refreshSession(sessionId, user);

        chain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response,
                           int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\": \"" + message + "\"}"
        );
    }

    private boolean isPublicPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/ws/")
                || path.equals("/ws")
                // Swagger UI
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui/")
                // OpenAPI spec
                || path.startsWith("/v3/api-docs")
                // Public API routes
                || path.startsWith("/api/v1/products/")
                || ("GET".equals(method)) && path.equals("/api/v1/products")
                || path.equals("/api/v1/settings")
                || path.startsWith("/api/v1/webhooks/")
                || path.equals("/api/v1/categories/all");
    }
}
