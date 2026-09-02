package com.eudext.erp.numbering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.numbering.internal.NumberingResetPolicy;
import com.eudext.erp.numbering.internal.SeriesConfigService;
import com.eudext.erp.testsupport.AbstractIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Epic 0.5 (PLAT-NUM) end to end against a real Postgres: naming-series
 * configuration (NUM-1), gapless allocation (NUM-2), fiscal-year reset
 * (NUM-3), and concurrency-safe allocation under load (NUM-4).
 */
class NumberingAllocationIT extends AbstractIntegrationTest {

    @Autowired
    private NumberingApi numberingApi;

    @Autowired
    private SeriesConfigService seriesConfigService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(tenantId);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void allocatesSequentialNumbersAgainstAConfiguredSeries() {
        seriesConfigService.configure(tenantId, companyId, "SALES_INVOICE", "SINV-", 5, NumberingResetPolicy.NEVER, 1);

        assertThat(numberingApi.allocateNumber(companyId, "SALES_INVOICE", LocalDate.of(2026, 1, 1))).isEqualTo("SINV-00001");
        assertThat(numberingApi.allocateNumber(companyId, "SALES_INVOICE", LocalDate.of(2026, 1, 2))).isEqualTo("SINV-00002");
        assertThat(numberingApi.allocateNumber(companyId, "SALES_INVOICE", LocalDate.of(2026, 1, 3))).isEqualTo("SINV-00003");
    }

    @Test
    void allocatingAgainstAnUnconfiguredDocTypeThrows() {
        assertThatThrownBy(() -> numberingApi.allocateNumber(companyId, "NO_SUCH_DOC_TYPE", LocalDate.now()))
                .isInstanceOf(NoActiveSeriesException.class);
    }

    @Test
    void deactivatedSeriesCannotBeAllocatedAgainst() {
        var series = seriesConfigService.configure(
                tenantId, companyId, "PURCHASE_INVOICE", "PINV-", 5, NumberingResetPolicy.NEVER, 1);
        seriesConfigService.deactivate(series.getId());

        assertThatThrownBy(() -> numberingApi.allocateNumber(companyId, "PURCHASE_INVOICE", LocalDate.now()))
                .isInstanceOf(NoActiveSeriesException.class);
    }

    @Test
    void annualFiscalYearResetRollsTheCounterOverAtTheBoundary() {
        seriesConfigService.configure(
                tenantId, companyId, "GOODS_RECEIPT_NOTE", "GRN-{FY}-", 5, NumberingResetPolicy.ANNUAL, 4);

        assertThat(numberingApi.allocateNumber(companyId, "GOODS_RECEIPT_NOTE", LocalDate.of(2026, 3, 31)))
                .isEqualTo("GRN-2025-26-00001");
        assertThat(numberingApi.allocateNumber(companyId, "GOODS_RECEIPT_NOTE", LocalDate.of(2026, 3, 31)))
                .isEqualTo("GRN-2025-26-00002");
        // rolls into the fiscal year starting April 2026 -> counter resets to 1
        assertThat(numberingApi.allocateNumber(companyId, "GOODS_RECEIPT_NOTE", LocalDate.of(2026, 4, 1)))
                .isEqualTo("GRN-2026-27-00001");
    }

    @Test
    void reconfiguringDoesNotResetTheLiveCounter() {
        seriesConfigService.configure(tenantId, companyId, "JOURNAL_VOUCHER", "JV-", 5, NumberingResetPolicy.NEVER, 1);
        assertThat(numberingApi.allocateNumber(companyId, "JOURNAL_VOUCHER", LocalDate.now())).isEqualTo("JV-00001");

        seriesConfigService.configure(tenantId, companyId, "JOURNAL_VOUCHER", "JRNL-", 6, NumberingResetPolicy.NEVER, 1);
        assertThat(numberingApi.allocateNumber(companyId, "JOURNAL_VOUCHER", LocalDate.now())).isEqualTo("JRNL-000002");
    }

    @Test
    void rollingBackTheCallersTransactionLeavesNoGap() {
        seriesConfigService.configure(tenantId, companyId, "PAYMENT_VOUCHER", "PAY-", 5, NumberingResetPolicy.NEVER, 1);
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> txTemplate.executeWithoutResult(status -> {
                    numberingApi.allocateNumber(companyId, "PAYMENT_VOUCHER", LocalDate.now());
                    throw new RuntimeException("simulated failure after allocating, before the document itself commits");
                }))
                .isInstanceOf(RuntimeException.class);

        // the number "burned" by the rolled-back transaction is handed out again -- no gap
        assertThat(numberingApi.allocateNumber(companyId, "PAYMENT_VOUCHER", LocalDate.now())).isEqualTo("PAY-00001");
    }

    @Test
    void concurrentAllocationsAgainstTheSameSeriesAreUniqueAndGapless() throws Exception {
        seriesConfigService.configure(tenantId, companyId, "SALES_INVOICE", "SINV-", 5, NumberingResetPolicy.NEVER, 1);
        int allocations = 40;
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            List<Callable<String>> tasks = java.util.stream.IntStream.range(0, allocations)
                    .<Callable<String>>mapToObj(i -> () -> {
                        TenantContext.set(tenantId);
                        try {
                            return numberingApi.allocateNumber(companyId, "SALES_INVOICE", LocalDate.now());
                        } finally {
                            TenantContext.clear();
                        }
                    })
                    .toList();
            List<Future<String>> futures = pool.invokeAll(tasks);
            List<String> results = futures.stream().map(this::join).toList();

            Set<String> distinct = Set.copyOf(results);
            assertThat(distinct).hasSize(allocations);
            Set<String> expected = java.util.stream.IntStream.rangeClosed(1, allocations)
                    .mapToObj(i -> "SINV-" + String.format("%05d", i))
                    .collect(Collectors.toSet());
            assertThat(distinct).isEqualTo(expected);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void seriesConfigCrudListsAndReflectsLifecycleChanges() {
        var created = seriesConfigService.configure(
                tenantId, companyId, "STOCK_ENTRY", "STE-", 5, NumberingResetPolicy.NEVER, 1);
        assertThat(seriesConfigService.seriesFor(companyId)).extracting("id").contains(created.getId());
        assertThat(seriesConfigService.getSeries(created.getId()).isActive()).isTrue();

        seriesConfigService.deactivate(created.getId());
        assertThat(seriesConfigService.getSeries(created.getId()).isActive()).isFalse();

        seriesConfigService.activate(created.getId());
        assertThat(seriesConfigService.getSeries(created.getId()).isActive()).isTrue();
    }

    private String join(Future<String> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
