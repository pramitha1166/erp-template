package com.eudext.erp.masterdata.internal.currency;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

    List<ExchangeRate> findByTenantIdAndCurrencyCodeOrderByRateDateDesc(UUID tenantId, String currencyCode);

    Optional<ExchangeRate> findByTenantIdAndCurrencyCodeAndRateDate(UUID tenantId, String currencyCode, LocalDate rateDate);

    /** MDM-8: the most recent rate on or before {@code asOf} — the rate in effect for a transaction dated {@code asOf}. */
    Optional<ExchangeRate> findFirstByTenantIdAndCurrencyCodeAndRateDateLessThanEqualOrderByRateDateDesc(
            UUID tenantId, String currencyCode, LocalDate asOf);
}
