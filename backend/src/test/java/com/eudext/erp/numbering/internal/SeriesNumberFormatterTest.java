package com.eudext.erp.numbering.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** NUM-1 / NUM-3: pure-function tests for date-part resolution and fiscal-year period keys. */
class SeriesNumberFormatterTest {

    @Test
    void neverPolicyHasNoPeriodKey() {
        assertThat(SeriesNumberFormatter.periodKeyFor(NumberingResetPolicy.NEVER, LocalDate.of(2026, 6, 15), 1))
                .isNull();
    }

    @Test
    void annualCalendarYearKeysByCalendarYear() {
        assertThat(SeriesNumberFormatter.periodKeyFor(NumberingResetPolicy.ANNUAL, LocalDate.of(2026, 1, 1), 1))
                .isEqualTo("2026");
        assertThat(SeriesNumberFormatter.periodKeyFor(NumberingResetPolicy.ANNUAL, LocalDate.of(2026, 12, 31), 1))
                .isEqualTo("2026");
    }

    @Test
    void annualFiscalYearKeysByFiscalYearStart() {
        // Fiscal year starting April: Jan-Mar belongs to the fiscal year that started the previous April.
        assertThat(SeriesNumberFormatter.periodKeyFor(NumberingResetPolicy.ANNUAL, LocalDate.of(2026, 3, 31), 4))
                .isEqualTo("2025");
        assertThat(SeriesNumberFormatter.periodKeyFor(NumberingResetPolicy.ANNUAL, LocalDate.of(2026, 4, 1), 4))
                .isEqualTo("2026");
        assertThat(SeriesNumberFormatter.periodKeyFor(NumberingResetPolicy.ANNUAL, LocalDate.of(2027, 3, 31), 4))
                .isEqualTo("2026");
    }

    @Test
    void resolvesDatePlaceholders() {
        LocalDate date = LocalDate.of(2026, 9, 5);
        assertThat(SeriesNumberFormatter.resolvePrefix("INV-{YYYY}-{MM}-", date, 1)).isEqualTo("INV-2026-09-");
        assertThat(SeriesNumberFormatter.resolvePrefix("INV-{YY}-", date, 1)).isEqualTo("INV-26-");
        assertThat(SeriesNumberFormatter.resolvePrefix("SINV-", date, 1)).isEqualTo("SINV-");
    }

    @Test
    void resolvesFiscalYearPlaceholderAsCalendarYearWhenFiscalYearStartsInJanuary() {
        assertThat(SeriesNumberFormatter.resolvePrefix("INV-{FY}-", LocalDate.of(2026, 6, 1), 1)).isEqualTo("INV-2026-");
    }

    @Test
    void resolvesFiscalYearPlaceholderAsRangeLabelForNonCalendarFiscalYear() {
        assertThat(SeriesNumberFormatter.resolvePrefix("INV-{FY}-", LocalDate.of(2026, 3, 1), 4)).isEqualTo("INV-2025-26-");
        assertThat(SeriesNumberFormatter.resolvePrefix("INV-{FY}-", LocalDate.of(2026, 4, 1), 4)).isEqualTo("INV-2026-27-");
    }

    @Test
    void zeroPadsToTheConfiguredWidth() {
        assertThat(SeriesNumberFormatter.zeroPad(1, 5)).isEqualTo("00001");
        assertThat(SeriesNumberFormatter.zeroPad(123456, 5)).isEqualTo("123456");
        assertThat(SeriesNumberFormatter.zeroPad(0, 1)).isEqualTo("0");
    }

    @Test
    void rejectsOutOfRangeFiscalYearStartMonth() {
        assertThatThrownBy(() -> SeriesNumberFormatter.periodKeyFor(NumberingResetPolicy.ANNUAL, LocalDate.now(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SeriesNumberFormatter.periodKeyFor(NumberingResetPolicy.ANNUAL, LocalDate.now(), 13))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
