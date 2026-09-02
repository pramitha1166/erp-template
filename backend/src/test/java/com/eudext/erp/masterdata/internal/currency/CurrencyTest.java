package com.eudext.erp.masterdata.internal.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** MDM-8: currency and exchange rate invariants. */
class CurrencyTest {

    private final UUID tenantId = UUID.randomUUID();

    @Test
    void createsAnEnabledCurrency() {
        Currency currency = Currency.create(tenantId, "LKR", "Sri Lankan Rupee", "Rs.", 2);
        assertThat(currency.isDisabled()).isFalse();
        assertThat(currency.getDecimalPlaces()).isEqualTo(2);
    }

    @Test
    void rejectsAnOutOfRangeDecimalPlaces() {
        assertThatThrownBy(() -> Currency.create(tenantId, "XYZ", "Bad Currency", null, 7)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Currency.create(tenantId, "XYZ", "Bad Currency", null, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exchangeRateRejectsANonPositiveRate() {
        assertThatThrownBy(() -> ExchangeRate.of(tenantId, "USD", LocalDate.now(), BigDecimal.ZERO, ExchangeRateSource.MANUAL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exchangeRateRecordsItsSource() {
        ExchangeRate rate = ExchangeRate.of(tenantId, "USD", LocalDate.of(2026, 1, 1), new BigDecimal("300.5"), ExchangeRateSource.CBSL);
        assertThat(rate.getSource()).isEqualTo(ExchangeRateSource.CBSL);
        assertThat(rate.getCurrencyCode()).isEqualTo("USD");
    }
}
