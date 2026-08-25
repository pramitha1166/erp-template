package com.eudext.erp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class CorsConfigTest {

    private CorsConfiguration configFor(List<String> patterns, String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        return new CorsConfig(patterns).corsConfigurationSource().getCorsConfiguration(request);
    }

    @Test
    void acceptsAnyOriginByDefault() {
        CorsConfiguration config = configFor(List.of("*"), "/api/documents");

        assertThat(config).isNotNull();
        assertThat(config.checkOrigin("http://13.233.255.21:3000")).isEqualTo("http://13.233.255.21:3000");
        assertThat(config.checkOrigin("https://anything.example.com")).isEqualTo("https://anything.example.com");
    }

    @Test
    void restrictsToConfiguredOriginsWhenSet() {
        CorsConfiguration config = configFor(List.of("https://erp.example.com"), "/api/documents");

        assertThat(config.checkOrigin("https://erp.example.com")).isEqualTo("https://erp.example.com");
        assertThat(config.checkOrigin("https://attacker.example.com")).isNull();
    }

    /**
     * The CORS spec forbids credentials alongside a wildcard origin, and
     * authentication here is a bearer token that needs none. Pinning this
     * stops a later change from quietly pairing the two.
     */
    @Test
    void neverAllowsCredentials() {
        assertThat(configFor(List.of("*"), "/api/documents").getAllowCredentials()).isNotEqualTo(Boolean.TRUE);
    }

    @Test
    void coversEveryPath() {
        assertThat(configFor(List.of("*"), "/actuator/health")).isNotNull();
        assertThat(configFor(List.of("*"), "/auth/login")).isNotNull();
    }
}
