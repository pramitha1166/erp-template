package com.eudext.erp.iam.internal.settings;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantSecuritySettingsRepository extends JpaRepository<TenantSecuritySettings, UUID> {

    Optional<TenantSecuritySettings> findByTenantId(UUID tenantId);
}
