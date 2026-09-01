package com.eudext.erp.numbering.internal;

import java.time.LocalDate;

/**
 * NUM-1 / NUM-3: pure functions for resolving a series' period key (the
 * fiscal-year-reset boundary) and rendering its date-part placeholders. No
 * persistence dependency, so behaviour at every boundary is exercised
 * directly by unit tests rather than through the allocation service.
 *
 * <p>Supported placeholders in a series prefix template: {@code {YYYY}}
 * (4-digit year), {@code {YY}} (2-digit year), {@code {MM}} (2-digit month),
 * {@code {FY}} (fiscal-year label, e.g. {@code 2025-26}). All are resolved
 * against the fiscal year the allocation date falls in when
 * {@code fiscalYearStartMonth > 1}, or the calendar year otherwise.
 */
final class SeriesNumberFormatter {

    private SeriesNumberFormatter() {}

    /** {@code null} means the series never resets (policy {@code NEVER}). */
    static String periodKeyFor(NumberingResetPolicy policy, LocalDate onDate, int fiscalYearStartMonth) {
        if (policy == NumberingResetPolicy.NEVER) {
            return null;
        }
        int fyStartYear = fiscalYearStart(onDate, fiscalYearStartMonth).getYear();
        return String.valueOf(fyStartYear);
    }

    static String resolvePrefix(String template, LocalDate onDate, int fiscalYearStartMonth) {
        LocalDate fyStart = fiscalYearStart(onDate, fiscalYearStartMonth);
        String yyyy = String.valueOf(onDate.getYear());
        String yy = yyyy.substring(2);
        String mm = String.format("%02d", onDate.getMonthValue());
        String fy = fiscalYearStartMonth == 1
                ? yyyy
                : String.valueOf(fyStart.getYear()) + "-" + String.format("%02d", (fyStart.getYear() + 1) % 100);
        return template.replace("{YYYY}", yyyy).replace("{YY}", yy).replace("{MM}", mm).replace("{FY}", fy);
    }

    static String zeroPad(long counter, int width) {
        return String.format("%0" + width + "d", counter);
    }

    private static LocalDate fiscalYearStart(LocalDate onDate, int fiscalYearStartMonth) {
        if (fiscalYearStartMonth < 1 || fiscalYearStartMonth > 12) {
            throw new IllegalArgumentException("fiscalYearStartMonth must be 1..12, got: " + fiscalYearStartMonth);
        }
        int year = onDate.getMonthValue() >= fiscalYearStartMonth ? onDate.getYear() : onDate.getYear() - 1;
        return LocalDate.of(year, fiscalYearStartMonth, 1);
    }
}
