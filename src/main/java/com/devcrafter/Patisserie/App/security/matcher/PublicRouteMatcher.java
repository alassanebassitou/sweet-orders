package com.devcrafter.Patisserie.App.security.matcher;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PublicRouteMatcher implements RequestMatcher {

    private static final List<RequestMatcher> COMMON_ROUTE = Stream.of(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/v3/api-docs",
                    "/api/v1/auth/**"
            ).map(pattern -> PathPatternRequestMatcher.withDefaults().matcher(pattern))
            .collect(Collectors.toList());
    private final RequestMatcher delegate;


    public PublicRouteMatcher(RequestMatcher... additionalMatcher) {
        List<RequestMatcher> matchers = new ArrayList<>(COMMON_ROUTE);
        matchers.addAll(List.of(additionalMatcher));
        delegate = new OrRequestMatcher(matchers);
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return delegate.matches(request);
    }
}
