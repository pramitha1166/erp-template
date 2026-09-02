package com.eudext.erp.masterdata.internal.fiscalyear;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** MDM-9: accounting period open/close administration. */
@Service
public class AccountingPeriodService {

    private final AccountingPeriodRepository accountingPeriodRepository;
    private final FiscalYearRepository fiscalYearRepository;

    public AccountingPeriodService(
            AccountingPeriodRepository accountingPeriodRepository, FiscalYearRepository fiscalYearRepository) {
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.fiscalYearRepository = fiscalYearRepository;
    }

    @Transactional(readOnly = true)
    public AccountingPeriod get(UUID periodId) {
        return accountingPeriodRepository.findById(periodId).orElseThrow(() -> new NoSuchElementException("No such accounting period"));
    }

    @Transactional(readOnly = true)
    public List<AccountingPeriod> listForFiscalYear(UUID fiscalYearId) {
        return accountingPeriodRepository.findByFiscalYearId(fiscalYearId);
    }

    @Transactional
    public void close(UUID periodId) {
        get(periodId).close();
    }

    /** Reopening a period only makes sense while its fiscal year is itself still open (or reopened first). */
    @Transactional
    public void reopen(UUID periodId) {
        AccountingPeriod period = get(periodId);
        FiscalYear fiscalYear = fiscalYearRepository
                .findById(period.getFiscalYearId())
                .orElseThrow(() -> new NoSuchElementException("No such fiscal year"));
        if (fiscalYear.getStatus() == FiscalYearStatus.CLOSED) {
            throw new IllegalStateException("Cannot reopen an accounting period whose fiscal year is closed");
        }
        period.reopen();
    }
}
