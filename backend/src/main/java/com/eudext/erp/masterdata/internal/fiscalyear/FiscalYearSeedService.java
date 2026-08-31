package com.eudext.erp.masterdata.internal.fiscalyear;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ADM-3: seeds one default open fiscal year with 12 monthly accounting periods. */
@Service
public class FiscalYearSeedService {

    private final FiscalYearRepository fiscalYearRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;

    public FiscalYearSeedService(
            FiscalYearRepository fiscalYearRepository, AccountingPeriodRepository accountingPeriodRepository) {
        this.fiscalYearRepository = fiscalYearRepository;
        this.accountingPeriodRepository = accountingPeriodRepository;
    }

    /** No-op if the company already has a fiscal year — seeding runs exactly once, at onboarding. */
    @Transactional
    public UUID seedDefault(UUID tenantId, UUID companyId, LocalDate fiscalYearStart) {
        if (fiscalYearRepository.existsByCompanyId(companyId)) {
            return fiscalYearRepository.findByCompanyId(companyId).get(0).getId();
        }
        LocalDate end = fiscalYearStart.plusYears(1).minusDays(1);
        String name = "FY " + fiscalYearStart.getYear() + "-" + end.getYear();
        FiscalYear fiscalYear = fiscalYearRepository.save(FiscalYear.of(tenantId, companyId, name, fiscalYearStart, end));

        LocalDate periodStart = fiscalYearStart;
        for (int i = 0; i < 12; i++) {
            LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
            String periodName = periodStart.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + periodStart.getYear();
            accountingPeriodRepository.save(
                    AccountingPeriod.of(tenantId, companyId, fiscalYear.getId(), periodName, periodStart, periodEnd));
            periodStart = periodStart.plusMonths(1);
        }
        return fiscalYear.getId();
    }
}
