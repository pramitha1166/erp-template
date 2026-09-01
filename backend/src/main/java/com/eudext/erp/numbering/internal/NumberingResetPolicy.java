package com.eudext.erp.numbering.internal;

/** NUM-3: whether and how a series' counter resets when the allocation date rolls into a new period. */
public enum NumberingResetPolicy {
    /** Counter never resets; it counts up for the life of the series. */
    NEVER,
    /** Counter resets to 1 at the start of each fiscal year (see {@code fiscalYearStartMonth}; month 1 is a calendar year). */
    ANNUAL
}
