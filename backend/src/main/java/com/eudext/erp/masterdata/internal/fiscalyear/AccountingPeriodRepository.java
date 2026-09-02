package com.eudext.erp.masterdata.internal.fiscalyear;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, UUID> {

    List<AccountingPeriod> findByFiscalYearId(UUID fiscalYearId);

    boolean existsByFiscalYearIdAndStatus(UUID fiscalYearId, FiscalYearStatus status);
}
