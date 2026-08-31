package com.eudext.erp.admin.internal.invite;

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

/** ADM-5 / BRD-14: a brand admin's invite for an additional or replacement tenant-admin user. Tenant-owned; RLS applies. */
@Entity
@Table(name = "tenant_admin_invites")
public class TenantAdminInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "email", nullable = false, updatable = false)
    private String email;

    @Column(name = "token_hash", nullable = false, updatable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InviteStatus status = InviteStatus.PENDING;

    @Column(name = "invited_by", nullable = false, updatable = false)
    private String invitedBy;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected TenantAdminInvite() {}

    public static TenantAdminInvite create(UUID tenantId, String email, String tokenHash, String invitedBy, Instant expiresAt) {
        TenantAdminInvite invite = new TenantAdminInvite();
        invite.tenantId = tenantId;
        invite.email = email;
        invite.tokenHash = tokenHash;
        invite.invitedBy = invitedBy;
        invite.expiresAt = expiresAt;
        return invite;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public InviteStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void accept() {
        this.status = InviteStatus.ACCEPTED;
        this.acceptedAt = Instant.now();
    }

    public void revoke() {
        this.status = InviteStatus.REVOKED;
    }

    public void expire() {
        this.status = InviteStatus.EXPIRED;
    }
}
