package com.eudext.erp.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cross-origin access for browser clients.
 *
 * <p>The deployed environments address the frontend and backend as separate
 * hosts, and in the dev environment those hosts are Fargate task IPs that
 * change on every deployment — so the default is to accept any origin. It is
 * a property rather than a literal: a production deployment sets
 * {@code app.cors.allowed-origin-patterns} to the real frontend host and
 * gets back the protection a same-origin policy is there to provide.
 *
 * <p>Credentials are deliberately not allowed. Authentication is a JWT
 * bearer token (IAM-1) with no cookies involved, so nothing needs them, and
 * the CORS spec forbids pairing credentials with a wildcard origin — a
 * later switch to cookie-based refresh tokens must revisit this together
 * with a non-wildcard origin list.
 */
@Configuration
public class CorsConfig {

    private final List<String> allowedOriginPatterns;

    public CorsConfig(@Value("${app.cors.allowed-origin-patterns:*}") List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOriginPatterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Location", "X-Correlation-Id"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
