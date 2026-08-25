package com.eudext.erp.iam.internal.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * IAM-1 / IAM-8: one row per issued refresh token. See V5 migration
 * comment for why only a hash of the token is stored.
 */
@Entity
@Table(name = "user_sessions")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "refresh_token_hash", nullable = false, updatable = false)
    private String refreshTokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_id")
    private UUID replacedById;

    protected UserSession() {}

    public static UserSession issue(
            UUID tenantId, UUID userId, String refreshTokenHash, Instant expiresAt, String ipAddress, String userAgent) {
        UserSession session = new UserSession();
        session.tenantId = tenantId;
        session.userId = userId;
        session.refreshTokenHash = refreshTokenHash;
        session.issuedAt = Instant.now();
        session.expiresAt = expiresAt;
        session.lastSeenAt = session.issuedAt;
        session.ipAddress = ipAddress;
        session.userAgent = userAgent;
        return session;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public UUID getReplacedById() {
        return replacedById;
    }

    public boolean wasRotated() {
        return replacedById != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    /** IAM-8: idle timeout is evaluated against activity, not just the token's absolute expiry. */
    public boolean isIdleTimedOut(int idleTimeoutMinutes) {
        return lastSeenAt.plusSeconds(idleTimeoutMinutes * 60L).isBefore(Instant.now());
    }

    public boolean isActive(int idleTimeoutMinutes) {
        return !isRevoked() && !isExpired() && !isIdleTimedOut(idleTimeoutMinutes);
    }

    public void touch() {
        this.lastSeenAt = Instant.now();
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    public void markRotatedTo(UUID newSessionId) {
        this.replacedById = newSessionId;
        revoke();
    }
}
