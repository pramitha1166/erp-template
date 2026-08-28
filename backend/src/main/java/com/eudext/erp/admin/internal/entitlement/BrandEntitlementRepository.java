package com.eudext.erp.admin.internal.entitlement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandEntitlementRepository extends JpaRepository<BrandEntitlement, UUID> {

    Optional<BrandEntitlement> findByBrandIdAndFeatureCode(UUID brandId, String featureCode);

    List<BrandEntitlement> findByBrandId(UUID brandId);
}
