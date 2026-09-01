package com.eudext.erp.numbering.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** NUM-1 / NUM-2 / NUM-3: in-memory behaviour of a series' allocate/configure logic, independent of persistence. */
class NumberingSeriesTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @Test
    void allocateIncrementsSequentiallyStartingAtOne() {
        NumberingSeries series = NumberingSeries.create(tenantId, companyId, "SALES_INVOICE", "SINV-", 5);

        assertThat(series.allocate(LocalDate.of(2026, 1, 1))).isEqualTo(1);
        assertThat(series.allocate(LocalDate.of(2026, 1, 2))).isEqualTo(2);
        assertThat(series.allocate(LocalDate.of(2026, 1, 3))).isEqualTo(3);
    }

    @Test
    void neverPolicyDoesNotResetAcrossYearBoundary() {
        NumberingSeries series = NumberingSeries.create(tenantId, companyId, "SALES_INVOICE", "SINV-", 5);
        series.configure("SINV-", 5, NumberingResetPolicy.NEVER, 1);

        series.allocate(LocalDate.of(2026, 12, 31));
        assertThat(series.allocate(LocalDate.of(2027, 1, 1))).isEqualTo(2);
    }

    @Test
    void annualPolicyResetsToOneOnNewFiscalYear() {
        NumberingSeries series = NumberingSeries.create(tenantId, companyId, "SALES_INVOICE", "SINV-", 5);
        series.configure("SINV-{FY}-", 5, NumberingResetPolicy.ANNUAL, 4);

        assertThat(series.allocate(LocalDate.of(2026, 3, 30))).isEqualTo(1);
        assertThat(series.allocate(LocalDate.of(2026, 3, 31))).isEqualTo(2);
        // rolls into fiscal year 2026-27 (starts April) -> counter resets
        assertThat(series.allocate(LocalDate.of(2026, 4, 1))).isEqualTo(1);
        assertThat(series.allocate(LocalDate.of(2026, 4, 2))).isEqualTo(2);
        // still within fiscal year 2026-27
        assertThat(series.allocate(LocalDate.of(2027, 3, 1))).isEqualTo(3);
    }

    @Test
    void resolvedPrefixReflectsConfiguredTemplate() {
        NumberingSeries series = NumberingSeries.create(tenantId, companyId, "SALES_INVOICE", "SINV-{YYYY}-", 5);
        assertThat(series.resolvedPrefix(LocalDate.of(2026, 9, 1))).isEqualTo("SINV-2026-");
    }

    @Test
    void configureRejectsOutOfRangeFiscalYearStartMonthWhenAnnual() {
        NumberingSeries series = NumberingSeries.create(tenantId, companyId, "SALES_INVOICE", "SINV-", 5);
        assertThatThrownBy(() -> series.configure("SINV-", 5, NumberingResetPolicy.ANNUAL, 13))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activateAndDeactivateToggleActiveFlag() {
        NumberingSeries series = NumberingSeries.create(tenantId, companyId, "SALES_INVOICE", "SINV-", 5);
        assertThat(series.isActive()).isTrue();
        series.deactivate();
        assertThat(series.isActive()).isFalse();
        series.activate();
        assertThat(series.isActive()).isTrue();
    }
}
