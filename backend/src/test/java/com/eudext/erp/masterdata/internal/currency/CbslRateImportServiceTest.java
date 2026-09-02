package com.eudext.erp.masterdata.internal.currency;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** MDM-8: the optional CBSL import job isolates each currency so one failure doesn't lose the rest of the batch. */
@ExtendWith(MockitoExtension.class)
class CbslRateImportServiceTest {

    @Mock
    private CbslRateSource rateSource;

    @Mock
    private CurrencyService currencyService;

    private CbslRateImportService service;
    private final UUID tenantId = UUID.randomUUID();
    private final LocalDate date = LocalDate.of(2026, 9, 1);

    @BeforeEach
    void setUp() {
        service = new CbslRateImportService(rateSource, currencyService);
    }

    @Test
    void recordsEveryFetchedRate() {
        when(rateSource.fetchRatesFor(date)).thenReturn(Map.of("USD", new BigDecimal("305.25"), "GBP", new BigDecimal("390.10")));

        service.importFor(tenantId, date);

        verify(currencyService).recordRate(tenantId, "USD", date, new BigDecimal("305.25"), ExchangeRateSource.CBSL);
        verify(currencyService).recordRate(tenantId, "GBP", date, new BigDecimal("390.10"), ExchangeRateSource.CBSL);
    }

    @Test
    void oneCurrencyFailingDoesNotStopTheOthersFromBeingRecorded() {
        when(rateSource.fetchRatesFor(date)).thenReturn(Map.of("USD", new BigDecimal("305.25"), "XXX", BigDecimal.TEN));
        doThrow(new NoSuchElementException("not enabled"))
                .when(currencyService)
                .recordRate(eq(tenantId), eq("XXX"), eq(date), any(), eq(ExchangeRateSource.CBSL));

        service.importFor(tenantId, date);

        verify(currencyService).recordRate(tenantId, "USD", date, new BigDecimal("305.25"), ExchangeRateSource.CBSL);
    }
}
