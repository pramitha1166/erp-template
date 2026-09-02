package com.eudext.erp.masterdata.internal.currency;

import com.eudext.erp.config.tenancy.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * MDM-8: pulls {@code date}'s rates from the configured {@link CbslRateSource} and records them for one tenant. Each
 * currency is recorded in its own transaction so one currency the tenant hasn't enabled (or any other per-currency
 * failure) doesn't roll back rates already recorded for the others.
 */
@Service
public class CbslRateImportService {

    private static final Logger log = LoggerFactory.getLogger(CbslRateImportService.class);

    private final CbslRateSource rateSource;
    private final CurrencyService currencyService;

    public CbslRateImportService(CbslRateSource rateSource, CurrencyService currencyService) {
        this.rateSource = rateSource;
        this.currencyService = currencyService;
    }

    public void importFor(UUID tenantId, LocalDate date) {
        TenantContext.set(tenantId);
        try {
            for (Map.Entry<String, BigDecimal> rate : rateSource.fetchRatesFor(date).entrySet()) {
                try {
                    currencyService.recordRate(tenantId, rate.getKey(), date, rate.getValue(), ExchangeRateSource.CBSL);
                } catch (RuntimeException e) {
                    log.error("CBSL rate import failed for tenant {} currency {}", tenantId, rate.getKey(), e);
                }
            }
        } finally {
            TenantContext.clear();
        }
    }
}
