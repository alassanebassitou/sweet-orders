package com.devcrafter.Patisserie.App.security.component;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CoopHeaderFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        // Fix Google OAuth popup communication
        httpResponse.setHeader(
                "Cross-Origin-Opener-Policy",
                "same-origin-allow-popups"
        );

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
