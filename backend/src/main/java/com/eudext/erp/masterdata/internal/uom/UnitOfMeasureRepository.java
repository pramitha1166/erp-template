package com.eudext.erp.masterdata.internal.uom;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, UUID> {

    List<UnitOfMeasure> findByTenantId(UUID tenantId);
}
