package com.eudext.erp.iam.internal.settings;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** IAM-8 / IAM-9: reads and updates a tenant's {@link SecurityPolicy}. */
@Service
public class TenantSecuritySettingsService {

    private final TenantSecuritySettingsRepository repository;

    public TenantSecuritySettingsService(TenantSecuritySettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SecurityPolicy resolve(UUID tenantId) {
        return repository.findByTenantId(tenantId).map(TenantSecuritySettings::toPolicy).orElseGet(SecurityPolicy::defaults);
    }

    @Transactional
    public SecurityPolicy update(UUID tenantId, SecurityPolicy policy) {
        TenantSecuritySettings settings = repository
                .findByTenantId(tenantId)
                .orElseGet(() -> TenantSecuritySettings.create(tenantId, policy));
        settings.applyFrom(policy);
        return repository.save(settings).toPolicy();
    }
}
