package com.eudext.erp.masterdata.internal.fiscalyear;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MDM-9: fiscal year administration beyond the onboarding seed (see {@link FiscalYearSeedService}) — closing a
 * fiscal year, or reopening one, with the invariant that a fiscal year can only be closed once every one of its
 * accounting periods is itself closed.
 */
@Service
public class FiscalYearService {

    private final FiscalYearRepository fiscalYearRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;

    public FiscalYearService(FiscalYearRepository fiscalYearRepository, AccountingPeriodRepository accountingPeriodRepository) {
        this.fiscalYearRepository = fiscalYearRepository;
        this.accountingPeriodRepository = accountingPeriodRepository;
    }

    @Transactional(readOnly = true)
    public FiscalYear get(UUID fiscalYearId) {
        return fiscalYearRepository.findById(fiscalYearId).orElseThrow(() -> new NoSuchElementException("No such fiscal year"));
    }

    @Transactional(readOnly = true)
    public List<FiscalYear> listForCompany(UUID companyId) {
        return fiscalYearRepository.findByCompanyId(companyId);
    }

    @Transactional
    public void close(UUID fiscalYearId) {
        if (accountingPeriodRepository.existsByFiscalYearIdAndStatus(fiscalYearId, FiscalYearStatus.OPEN)) {
            throw new IllegalStateException("Cannot close a fiscal year while it still has an open accounting period");
        }
        get(fiscalYearId).close();
    }

    @Transactional
    public void reopen(UUID fiscalYearId) {
        get(fiscalYearId).reopen();
    }
}
