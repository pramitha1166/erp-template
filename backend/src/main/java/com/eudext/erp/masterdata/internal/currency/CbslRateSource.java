package com.eudext.erp.masterdata.internal.currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * MDM-8: the optional Central Bank of Sri Lanka rate feed. Manual entry ({@link CurrencyService#recordRate}) is the
 * baseline the SRS requires; this is the "optional... import" half. No environment is wired to a real CBSL endpoint
 * yet — {@link CbslRateImportScheduler} only calls this when {@code eudext.masterdata.currency.cbsl-import.enabled}
 * is turned on, against whatever implementation an environment supplies.
 */
public interface CbslRateSource {

    /** Rates to the base currency for {@code date}, keyed by ISO 4217 currency code. Empty if none are published yet. */
    Map<String, BigDecimal> fetchRatesFor(LocalDate date);
}
