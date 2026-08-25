package com.eudext.erp.iam.internal.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IAM-1: JWT signing key and token lifetimes. {@code secret} defaults to a
 * fixed dev-only value (same convention as the datasource password
 * defaults in application.yml) — every real environment must override
 * {@code JWT_SECRET} with a random, sufficiently long value; a short or
 * predictable secret defeats HS256 entirely.
 */
@ConfigurationProperties(prefix = "eudext.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {

    public JwtProperties {
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(15);
        }
        if (refreshTokenTtl == null) {
            refreshTokenTtl = Duration.ofDays(7);
        }
    }
}
