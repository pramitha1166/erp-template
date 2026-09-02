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

/** MDM-9: a fiscal year can only be closed once every one of its accounting periods is itself closed. */
@ExtendWith(MockitoExtension.class)
class FiscalYearServiceTest {

    @Mock
    private FiscalYearRepository fiscalYearRepository;

    @Mock
    private AccountingPeriodRepository accountingPeriodRepository;

    private FiscalYearService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FiscalYearService(fiscalYearRepository, accountingPeriodRepository);
    }

    @Test
    void closeIsRejectedWhileAnAccountingPeriodIsStillOpen() {
        UUID fiscalYearId = UUID.randomUUID();
        when(accountingPeriodRepository.existsByFiscalYearIdAndStatus(fiscalYearId, FiscalYearStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> service.close(fiscalYearId)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void closeSucceedsOnceEveryPeriodIsClosed() {
        FiscalYear fiscalYear = FiscalYear.of(tenantId, companyId, "FY 2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        UUID fiscalYearId = UUID.randomUUID();
        setId(fiscalYear, fiscalYearId);
        when(accountingPeriodRepository.existsByFiscalYearIdAndStatus(fiscalYearId, FiscalYearStatus.OPEN)).thenReturn(false);
        when(fiscalYearRepository.findById(fiscalYearId)).thenReturn(Optional.of(fiscalYear));

        service.close(fiscalYearId);

        assertThat(fiscalYear.getStatus()).isEqualTo(FiscalYearStatus.CLOSED);
    }

    @Test
    void reopenSetsStatusBackToOpen() {
        FiscalYear fiscalYear = FiscalYear.of(tenantId, companyId, "FY 2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        fiscalYear.close();
        UUID fiscalYearId = UUID.randomUUID();
        setId(fiscalYear, fiscalYearId);
        when(fiscalYearRepository.findById(fiscalYearId)).thenReturn(Optional.of(fiscalYear));

        service.reopen(fiscalYearId);

        assertThat(fiscalYear.getStatus()).isEqualTo(FiscalYearStatus.OPEN);
    }

    private static void setId(FiscalYear fiscalYear, UUID id) {
        try {
            var idField = FiscalYear.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(fiscalYear, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
