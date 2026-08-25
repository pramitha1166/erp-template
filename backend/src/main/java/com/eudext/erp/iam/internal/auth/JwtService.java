package com.eudext.erp.iam.internal.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * IAM-1 / IAM-2: issues and parses JWTs. Two distinct purposes share this
 * signer but are never interchangeable — a {@code purpose} claim ties each
 * token to the one flow it's valid for, so a leaked MFA challenge token
 * (issued after password verification, before the TOTP code) can never be
 * replayed as a full access token. Refresh tokens are opaque random
 * strings tracked in {@code user_sessions} (see {@code SessionService}),
 * not JWTs — that's what makes revocation and reuse-detection possible
 * without a token blocklist.
 */
@Service
public class JwtService {

    private static final String CLAIM_TENANT_ID = "tid";
    private static final String CLAIM_PURPOSE = "purpose";
    private static final String CLAIM_ACTOR = "act";
    private static final String PURPOSE_ACCESS = "access";
    private static final String PURPOSE_MFA = "mfa";
    private static final String PURPOSE_IMPERSONATION = "impersonation";
    private static final Duration MFA_CHALLENGE_TTL = Duration.ofMinutes(5);

    private final SecretKey signingKey;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(UUID userId, UUID tenantId) {
        return issue(userId, tenantId, PURPOSE_ACCESS, properties.accessTokenTtl());
    }

    public Optional<AccessTokenClaims> parseAccessToken(String token) {
        return parse(token, PURPOSE_ACCESS);
    }

    /** IAM-2: issued once a password has checked out but before the TOTP code has been verified. */
    public String issueMfaChallenge(UUID userId, UUID tenantId) {
        return issue(userId, tenantId, PURPOSE_MFA, MFA_CHALLENGE_TTL);
    }

    public Optional<AccessTokenClaims> parseMfaChallenge(String token) {
        return parse(token, PURPOSE_MFA);
    }

    /**
     * ADM-7: a time-boxed token letting {@code actorUserId} (a platform or
     * brand admin) act as {@code targetUserId} within {@code
     * targetTenantId}. Carries an extra {@code act} claim naming the real
     * admin so {@link #parseImpersonationToken} can hand it back to the
     * caller for audit tagging — the {@code sub}/{@code tid} claims stay
     * the target's, so every ordinary permission check downstream sees
     * exactly what it would for a real login as that user.
     */
    public String issueImpersonationToken(UUID actorUserId, UUID targetUserId, UUID targetTenantId, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(targetUserId.toString())
                .claim(CLAIM_TENANT_ID, targetTenantId.toString())
                .claim(CLAIM_PURPOSE, PURPOSE_IMPERSONATION)
                .claim(CLAIM_ACTOR, actorUserId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey)
                .compact();
    }

    public Optional<ImpersonationClaims> parseImpersonationToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
            if (!PURPOSE_IMPERSONATION.equals(claims.get(CLAIM_PURPOSE, String.class))) {
                return Optional.empty();
            }
            UUID targetUserId = UUID.fromString(claims.getSubject());
            UUID targetTenantId = UUID.fromString(claims.get(CLAIM_TENANT_ID, String.class));
            UUID actorUserId = UUID.fromString(claims.get(CLAIM_ACTOR, String.class));
            return Optional.of(new ImpersonationClaims(actorUserId, targetUserId, targetTenantId));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String issue(UUID userId, UUID tenantId, String purpose, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TENANT_ID, tenantId.toString())
                .claim(CLAIM_PURPOSE, purpose)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey)
                .compact();
    }

    private Optional<AccessTokenClaims> parse(String token, String expectedPurpose) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
            if (!expectedPurpose.equals(claims.get(CLAIM_PURPOSE, String.class))) {
                return Optional.empty();
            }
            UUID userId = UUID.fromString(claims.getSubject());
            UUID tenantId = UUID.fromString(claims.get(CLAIM_TENANT_ID, String.class));
            return Optional.of(new AccessTokenClaims(userId, tenantId));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record AccessTokenClaims(UUID userId, UUID tenantId) {}

    public record ImpersonationClaims(UUID actorUserId, UUID targetUserId, UUID targetTenantId) {}
}
