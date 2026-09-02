package com.eudext.erp.masterdata.internal.fiscalyear;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** MDM-9: an accounting period cannot be reopened while its fiscal year is closed. */
@ExtendWith(MockitoExtension.class)
class AccountingPeriodServiceTest {

    @Mock
    private AccountingPeriodRepository accountingPeriodRepository;

    @Mock
    private FiscalYearRepository fiscalYearRepository;

    private AccountingPeriodService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AccountingPeriodService(accountingPeriodRepository, fiscalYearRepository);
    }

    @Test
    void reopenIsRejectedWhenTheFiscalYearIsClosed() {
        FiscalYear fiscalYear = FiscalYear.of(tenantId, companyId, "FY 2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        fiscalYear.close();
        UUID fiscalYearId = setId(fiscalYear);

        AccountingPeriod period =
                AccountingPeriod.of(tenantId, companyId, fiscalYearId, "January 2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        period.close();
        UUID periodId = setId(period);

        when(accountingPeriodRepository.findById(periodId)).thenReturn(Optional.of(period));
        when(fiscalYearRepository.findById(fiscalYearId)).thenReturn(Optional.of(fiscalYear));

        assertThatThrownBy(() -> service.reopen(periodId)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reopenSucceedsWhenTheFiscalYearIsStillOpen() {
        FiscalYear fiscalYear = FiscalYear.of(tenantId, companyId, "FY 2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        UUID fiscalYearId = setId(fiscalYear);

        AccountingPeriod period =
                AccountingPeriod.of(tenantId, companyId, fiscalYearId, "January 2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        period.close();
        UUID periodId = setId(period);

        when(accountingPeriodRepository.findById(periodId)).thenReturn(Optional.of(period));
        when(fiscalYearRepository.findById(fiscalYearId)).thenReturn(Optional.of(fiscalYear));

        service.reopen(periodId);

        assertThat(period.getStatus()).isEqualTo(FiscalYearStatus.OPEN);
    }

    private static UUID setId(FiscalYear fiscalYear) {
        UUID id = UUID.randomUUID();
        try {
            var idField = FiscalYear.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(fiscalYear, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return id;
    }

    private static UUID setId(AccountingPeriod period) {
        UUID id = UUID.randomUUID();
        try {
            var idField = AccountingPeriod.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(period, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return id;
    }
}
