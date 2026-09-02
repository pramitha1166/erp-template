package com.eudext.erp.masterdata.internal.currency;

import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * MDM-8: the optional CBSL rate import job. {@code @EnableScheduling} lives in the {@code scheduler} module
 * (com.eudext.erp.scheduler.internal.SchedulingConfig).
 */
@Component
class CbslRateImportScheduler {

    private final CbslRateImportService importService;
    private final CbslImportProperties properties;

    CbslRateImportScheduler(CbslRateImportService importService, CbslImportProperties properties) {
        this.importService = importService;
        this.properties = properties;
    }

    @Scheduled(cron = "${eudext.masterdata.currency.cbsl-import.cron:0 0 7 * * *}")
    void importTodaysRates() {
        if (!properties.isEnabled() || properties.getTenantIds().isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        properties.getTenantIds().forEach(tenantId -> importService.importFor(tenantId, today));
    }
}
