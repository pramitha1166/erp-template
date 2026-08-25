package com.eudext.erp.audit.internal.archive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AUD-5: daily sweep. See {@link AuditArchiveProperties#getTenantIds()} for
 * why this is a configured list rather than a real cross-tenant scan — RLS
 * (ARCH-2) fail-closes to zero rows for any query issued without a tenant
 * in context, so "enumerate every tenant" isn't something any job can do
 * yet, audit archival included, until Epic 0.9 adds a tenant registry.
 * {@code @EnableScheduling} lives in the {@code scheduler} module
 * (com.eudext.erp.scheduler.internal.SchedulingConfig) — that placeholder
 * module exists precisely to own turning Spring's scheduling on.
 */
@Component
class AuditArchivalScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuditArchivalScheduler.class);

    private final AuditArchiveService archiveService;
    private final AuditArchiveProperties properties;

    AuditArchivalScheduler(AuditArchiveService archiveService, AuditArchiveProperties properties) {
        this.archiveService = archiveService;
        this.properties = properties;
    }

    @Scheduled(cron = "${eudext.audit.archive.cron:0 0 3 * * *}")
    void archiveDueTenants() {
        if (!properties.isEnabled() || properties.getTenantIds().isEmpty()) {
            return;
        }
        properties.getTenantIds().forEach(tenantId -> {
            try {
                archiveService.archiveForTenant(tenantId);
            } catch (RuntimeException e) {
                log.error("Audit archival failed for tenant {}", tenantId, e);
            }
        });
    }
}
