package com.eudext.erp.masterdata.internal.costcentre;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostCentreRepository extends JpaRepository<CostCentre, UUID> {

    List<CostCentre> findByCompanyId(UUID companyId);
}
