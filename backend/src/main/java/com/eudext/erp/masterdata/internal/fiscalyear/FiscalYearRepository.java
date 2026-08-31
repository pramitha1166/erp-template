package com.eudext.erp.masterdata.internal.fiscalyear;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalYearRepository extends JpaRepository<FiscalYear, UUID> {

    List<FiscalYear> findByCompanyId(UUID companyId);

    boolean existsByCompanyId(UUID companyId);
}
