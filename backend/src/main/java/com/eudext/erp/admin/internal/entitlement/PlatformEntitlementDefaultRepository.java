package com.eudext.erp.admin.internal.entitlement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformEntitlementDefaultRepository extends JpaRepository<PlatformEntitlementDefault, UUID> {

    Optional<PlatformEntitlementDefault> findByFeatureCode(String featureCode);

    List<PlatformEntitlementDefault> findAllByOrderByFeatureCode();
}
