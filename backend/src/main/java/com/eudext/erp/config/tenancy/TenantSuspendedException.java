package com.eudext.erp.config.tenancy;

import java.util.UUID;

/** ADM-6: thrown wherever a suspended tenant attempts a login or a blocked write. */
public class TenantSuspendedException extends RuntimeException {

    private final UUID tenantId;

    public TenantSuspendedException(UUID tenantId) {
        super("Tenant is suspended: " + tenantId);
        this.tenantId = tenantId;
    }

    public UUID getTenantId() {
        return tenantId;
    }
}
