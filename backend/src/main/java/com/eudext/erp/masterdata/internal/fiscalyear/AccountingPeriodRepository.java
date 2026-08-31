package com.eudext.erp.masterdata.internal.fiscalyear;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, UUID> {}
