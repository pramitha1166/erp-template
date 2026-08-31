package com.eudext.erp.admin.internal.entitlement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantEntitlementRepository extends JpaRepository<TenantEntitlement, UUID> {

    Optional<TenantEntitlement> findByTenantIdAndFeatureCode(UUID tenantId, String featureCode);

    List<TenantEntitlement> findByTenantId(UUID tenantId);
}
