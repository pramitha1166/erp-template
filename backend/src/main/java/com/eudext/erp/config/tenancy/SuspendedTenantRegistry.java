package com.eudext.erp.config.tenancy;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADM-6: shared read/write point for tenant suspension state. {@code admin}
 * calls {@link #suspend}/{@link #reactivate} when an operator acts on a
 * tenant; {@code iam} calls {@link #requireActive} before issuing a login
 * token. See {@link SuspendedTenantMarker} for why this lives here rather
 * than on the {@code admin} module's own {@code Tenant} entity.
 */
@Component
public class SuspendedTenantRegistry {

    private final SuspendedTenantRepository repository;

    public SuspendedTenantRegistry(SuspendedTenantRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isSuspended(UUID tenantId) {
        return repository.existsById(tenantId);
    }

    /** Throws {@link TenantSuspendedException} if the tenant is currently suspended; otherwise a no-op. */
    @Transactional(readOnly = true)
    public void requireActive(UUID tenantId) {
        if (isSuspended(tenantId)) {
            throw new TenantSuspendedException(tenantId);
        }
    }

    @Transactional
    public void suspend(UUID tenantId, String reason) {
        if (!repository.existsById(tenantId)) {
            repository.save(SuspendedTenantMarker.of(tenantId, reason));
        }
    }

    @Transactional
    public void reactivate(UUID tenantId) {
        repository.deleteById(tenantId);
    }
}
