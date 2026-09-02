package com.eudext.erp.masterdata.internal.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** MDM-8: recording exchange rates only against a currency the tenant has enabled. */
@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    private CurrencyService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CurrencyService(currencyRepository, exchangeRateRepository);
    }

    @Test
    void recordRateRejectsAnUnknownCurrencyCode() {
        when(currencyRepository.findByTenantIdAndCode(tenantId, "USD")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordRate(tenantId, "USD", LocalDate.now(), BigDecimal.TEN, ExchangeRateSource.MANUAL))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void recordRateRejectsADisabledCurrency() {
        Currency usd = Currency.create(tenantId, "USD", "US Dollar", "$", 2);
        usd.disable();
        when(currencyRepository.findByTenantIdAndCode(tenantId, "USD")).thenReturn(Optional.of(usd));

        assertThatThrownBy(() -> service.recordRate(tenantId, "USD", LocalDate.now(), BigDecimal.TEN, ExchangeRateSource.MANUAL))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recordRateOverwritesAnExistingRateForTheSameDate() {
        Currency usd = Currency.create(tenantId, "USD", "US Dollar", "$", 2);
        when(currencyRepository.findByTenantIdAndCode(tenantId, "USD")).thenReturn(Optional.of(usd));
        ExchangeRate existing = ExchangeRate.of(tenantId, "USD", LocalDate.of(2026, 1, 1), BigDecimal.TEN, ExchangeRateSource.MANUAL);
        when(exchangeRateRepository.findByTenantIdAndCurrencyCodeAndRateDate(tenantId, "USD", LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(existing));
        when(exchangeRateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExchangeRate result =
                service.recordRate(tenantId, "USD", LocalDate.of(2026, 1, 1), new BigDecimal("305.25"), ExchangeRateSource.MANUAL);

        assertThat(result.getRateToBase()).isEqualByComparingTo(new BigDecimal("305.25"));
    }
}
