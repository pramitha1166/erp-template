package com.eudext.erp.admin.internal.impersonation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** ADM-7: a record of a time-boxed platform/brand-admin-as-tenant-admin session. Tenant-owned (the target tenant); RLS applies. */
@Entity
@Table(name = "impersonation_sessions")
public class ImpersonationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;

    @Column(name = "target_user_id", nullable = false, updatable = false)
    private UUID targetUserId;

    @Column(name = "reason", nullable = false, updatable = false)
    private String reason;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ImpersonationSessionStatus status = ImpersonationSessionStatus.ACTIVE;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ImpersonationSession() {}

    public static ImpersonationSession start(UUID tenantId, UUID actorUserId, UUID targetUserId, String reason, Instant expiresAt) {
        ImpersonationSession session = new ImpersonationSession();
        session.tenantId = tenantId;
        session.actorUserId = actorUserId;
        session.targetUserId = targetUserId;
        session.reason = reason;
        session.startedAt = Instant.now();
        session.expiresAt = expiresAt;
        return session;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public ImpersonationSessionStatus getStatus() {
        return status;
    }

    public void end() {
        this.status = ImpersonationSessionStatus.ENDED;
        this.endedAt = Instant.now();
    }
}
