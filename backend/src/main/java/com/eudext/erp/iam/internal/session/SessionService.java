package com.eudext.erp.iam.internal.session;

import com.eudext.erp.iam.internal.settings.TenantSecuritySettingsService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM-1 / IAM-8: issues, rotates, lists, and revokes refresh-token-backed
 * sessions. Rotation-with-reuse-detection here is what makes a stolen
 * refresh token detectable: presenting an already-rotated token revokes
 * every session the user has (see {@link RefreshTokenReuseDetectedException}).
 */
@Service
public class SessionService {

    private static final int RAW_TOKEN_BYTES = 32;

    private final UserSessionRepository repository;
    private final TenantSecuritySettingsService settingsService;
    private final SecureRandom secureRandom = new SecureRandom();

    public SessionService(UserSessionRepository repository, TenantSecuritySettingsService settingsService) {
        this.repository = repository;
        this.settingsService = settingsService;
    }

    public record IssuedSession(UUID sessionId, String rawRefreshToken, Instant expiresAt) {}

    public record RotationResult(UUID userId, UUID tenantId, IssuedSession issued) {}

    @Transactional
    public IssuedSession issue(UUID tenantId, UUID userId, String ipAddress, String userAgent, Duration ttl) {
        String rawToken = generateRawToken(tenantId);
        Instant expiresAt = Instant.now().plus(ttl);
        UserSession session = UserSession.issue(tenantId, userId, hash(rawToken), expiresAt, ipAddress, userAgent);
        UserSession saved = repository.save(session);
        return new IssuedSession(saved.getId(), rawToken, expiresAt);
    }

    /**
     * The raw refresh token embeds its tenant id as a prefix (see {@link
     * #generateRawToken}) precisely so callers can learn which tenant to
     * set on {@code TenantContext} — and therefore which RLS-scoped rows
     * become visible — *before* looking the token up. Without this, a
     * refresh call would have the same chicken-and-egg problem login does
     * (see the V4 migration comment), except refresh has no login form to
     * additionally collect a tenant id from.
     */
    public static UUID extractTenantId(String rawRefreshToken) {
        int separator = rawRefreshToken.indexOf(':');
        if (separator < 0) {
            throw new InvalidRefreshTokenException();
        }
        try {
            return UUID.fromString(rawRefreshToken.substring(0, separator));
        } catch (IllegalArgumentException e) {
            throw new InvalidRefreshTokenException();
        }
    }

    /**
     * Validates and rotates a refresh token. Returns the new session
     * issued in its place; the caller mints a new JWT/refresh token pair
     * from that. Assumes {@code TenantContext} is already set to the
     * token's embedded tenant (see {@link #extractTenantId}).
     */
    @Transactional
    public RotationResult rotate(String rawRefreshToken, String ipAddress, String userAgent, Duration ttl) {
        UserSession existing = repository
                .findByRefreshTokenHash(hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (existing.isRevoked() || existing.wasRotated()) {
            revokeAll(existing.getUserId());
            throw new RefreshTokenReuseDetectedException();
        }

        int idleTimeoutMinutes = settingsService.resolve(existing.getTenantId()).idleTimeoutMinutes();
        if (existing.isExpired() || existing.isIdleTimedOut(idleTimeoutMinutes)) {
            throw new InvalidRefreshTokenException();
        }

        existing.touch();
        IssuedSession next = issue(existing.getTenantId(), existing.getUserId(), ipAddress, userAgent, ttl);
        existing.markRotatedTo(next.sessionId());
        repository.save(existing);
        return new RotationResult(existing.getUserId(), existing.getTenantId(), next);
    }

    @Transactional(readOnly = true)
    public List<UserSession> listActive(UUID userId) {
        return repository.findByUserId(userId).stream()
                .filter(s -> s.isActive(settingsService.resolve(s.getTenantId()).idleTimeoutMinutes()))
                .toList();
    }

    /** IAM-1: logout — revokes the one session this token belongs to, silently no-op'ing if it's already gone. */
    @Transactional
    public void revokeByToken(String rawRefreshToken) {
        repository.findByRefreshTokenHash(hash(rawRefreshToken)).ifPresent(session -> {
            session.revoke();
            repository.save(session);
        });
    }

    @Transactional
    public void revoke(UUID sessionId) {
        repository.findById(sessionId).ifPresent(session -> {
            session.revoke();
            repository.save(session);
        });
    }

    @Transactional
    public void revokeAll(UUID userId) {
        repository.findByUserId(userId).forEach(session -> {
            if (!session.isRevoked()) {
                session.revoke();
                repository.save(session);
            }
        });
    }

    private String generateRawToken(UUID tenantId) {
        byte[] bytes = new byte[RAW_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return tenantId + ":" + secret;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
