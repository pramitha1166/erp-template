package com.eudext.erp.config.tenancy;

import java.util.Optional;
import java.util.UUID;

/**
 * ARCH-2: holds the tenant the current thread is acting on behalf of. This
 * is the single source {@link TenantAwareDataSource} reads from to set the
 * {@code app.tenant_id} Postgres session variable every RLS policy checks.
 *
 * <p>Nothing populates this from an inbound request yet — that is Epic
 * 0.2's job: once IAM-1 wires JWT authentication, a request filter running
 * after authentication resolves the tenant claim and calls {@link #set}
 * for the duration of the request (mirroring {@code CorrelationIdFilter}),
 * clearing it in a {@code finally}. Trusting an unauthenticated,
 * client-supplied value here — e.g. a plain HTTP header — would defeat the
 * entire point of RLS, so that wiring is deliberately not added until
 * there is a validated identity to resolve it from. Until then, callers
 * (services, tests) set it explicitly.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) {
        if (tenantId == null) {
            CURRENT_TENANT.remove();
        } else {
            CURRENT_TENANT.set(tenantId);
        }
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
