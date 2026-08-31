package com.eudext.erp.admin.internal.tenant;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    List<Tenant> findByBrandId(UUID brandId);
}
