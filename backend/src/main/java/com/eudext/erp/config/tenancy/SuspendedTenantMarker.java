package com.eudext.erp.config.tenancy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * ADM-6: presence of a row means the tenant is currently suspended — new
 * logins and (once transactional modules exist to check it) new postings
 * are blocked. Deliberately not the {@code admin} module's {@code Tenant}
 * entity itself: {@code iam} needs to check this at login time, and
 * {@code iam} must never depend on {@code admin} (ARCH-1 forbids the
 * resulting module cycle, since {@code admin} already depends on {@code
 * iam} for user provisioning). Living in {@code config.tenancy} — the
 * shared, dependency-free module both sides already depend on — keeps the
 * DAG one-directional: {@code admin} writes this row when it suspends/
 * reactivates a tenant, {@code iam} only ever reads it.
 *
 * <p>Keyed on {@code tenant_id} directly (opaque, same convention as {@code
 * users.tenant_id}) with the same RLS shape as {@code users}: the caller-
 * supplied tenant id at login time is exactly what this check needs to be
 * scoped by.
 */
@Entity
@Table(name = "suspended_tenants")
public class SuspendedTenantMarker {

    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "suspended_at", nullable = false, updatable = false)
    private Instant suspendedAt;

    @Column(name = "reason")
    private String reason;

    protected SuspendedTenantMarker() {}

    public static SuspendedTenantMarker of(UUID tenantId, String reason) {
        SuspendedTenantMarker marker = new SuspendedTenantMarker();
        marker.tenantId = tenantId;
        marker.suspendedAt = Instant.now();
        marker.reason = reason;
        return marker;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public Instant getSuspendedAt() {
        return suspendedAt;
    }

    public String getReason() {
        return reason;
    }
}
