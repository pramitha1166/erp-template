package com.eudext.erp.masterdata.internal.currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** MDM-8: currency master and date-effective exchange rates — manual entry; the CBSL import job records with {@code CBSL}. */
@Service
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public CurrencyService(CurrencyRepository currencyRepository, ExchangeRateRepository exchangeRateRepository) {
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @Transactional
    public Currency create(UUID tenantId, String code, String name, String symbol, int decimalPlaces) {
        return currencyRepository.save(Currency.create(tenantId, code, name, symbol, decimalPlaces));
    }

    @Transactional
    public void disable(UUID currencyId) {
        get(currencyId).disable();
    }

    @Transactional
    public void enable(UUID currencyId) {
        get(currencyId).enable();
    }

    @Transactional(readOnly = true)
    public Currency get(UUID currencyId) {
        return currencyRepository.findById(currencyId).orElseThrow(() -> new NoSuchElementException("No such currency"));
    }

    @Transactional(readOnly = true)
    public List<Currency> listForTenant(UUID tenantId) {
        return currencyRepository.findByTenantId(tenantId);
    }

    /** MDM-8: records (or overwrites) the rate for {@code currencyCode} effective on {@code rateDate}. */
    @Transactional
    public ExchangeRate recordRate(
            UUID tenantId, String currencyCode, LocalDate rateDate, BigDecimal rateToBase, ExchangeRateSource source) {
        requireEnabledCurrency(tenantId, currencyCode);
        exchangeRateRepository
                .findByTenantIdAndCurrencyCodeAndRateDate(tenantId, currencyCode, rateDate)
                .ifPresent(exchangeRateRepository::delete);
        return exchangeRateRepository.save(ExchangeRate.of(tenantId, currencyCode, rateDate, rateToBase, source));
    }

    @Transactional(readOnly = true)
    public List<ExchangeRate> history(UUID tenantId, String currencyCode) {
        return exchangeRateRepository.findByTenantIdAndCurrencyCodeOrderByRateDateDesc(tenantId, currencyCode);
    }

    /** MDM-8: the rate in effect for a transaction dated {@code asOf} — the most recent rate on or before that date. */
    @Transactional(readOnly = true)
    public ExchangeRate rateAsOf(UUID tenantId, String currencyCode, LocalDate asOf) {
        return exchangeRateRepository
                .findFirstByTenantIdAndCurrencyCodeAndRateDateLessThanEqualOrderByRateDateDesc(tenantId, currencyCode, asOf)
                .orElseThrow(() -> new NoSuchElementException("No exchange rate recorded for " + currencyCode + " on or before " + asOf));
    }

    private void requireEnabledCurrency(UUID tenantId, String currencyCode) {
        Currency currency = currencyRepository
                .findByTenantIdAndCode(tenantId, currencyCode)
                .orElseThrow(() -> new NoSuchElementException("No such currency: " + currencyCode));
        if (currency.isDisabled()) {
            throw new IllegalStateException("Currency " + currencyCode + " is disabled");
        }
    }
}
