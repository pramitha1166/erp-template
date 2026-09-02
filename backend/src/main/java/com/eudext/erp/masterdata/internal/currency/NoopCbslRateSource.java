package com.eudext.erp.masterdata.internal.currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * The default {@link CbslRateSource} — always empty. No real CBSL client ships yet (see {@link CbslRateSource}'s
 * javadoc); an environment that wants the import job to do real work supplies its own {@link CbslRateSource} bean,
 * which Spring picks up ahead of this one.
 */
@Component
@ConditionalOnMissingBean(CbslRateSource.class)
class NoopCbslRateSource implements CbslRateSource {

    @Override
    public Map<String, BigDecimal> fetchRatesFor(LocalDate date) {
        return Map.of();
    }
}
