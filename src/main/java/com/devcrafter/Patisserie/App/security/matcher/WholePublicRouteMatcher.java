package com.devcrafter.Patisserie.App.security.matcher;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;

@Component
public class WholePublicRouteMatcher extends PublicRouteMatcher{

    public WholePublicRouteMatcher() {
        super(
                PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/webhooks/**"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/v1/categories/all"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/v1/zones-livraison/**"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/v1/settings")
        );
    }
}
