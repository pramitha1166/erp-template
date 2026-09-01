package com.eudext.erp.workflow.internal.escalation;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.workflow.internal.engine.WorkflowEngine;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * WF-5: periodic sweep for timed-out approval tasks. {@code @EnableScheduling}
 * lives in the {@code scheduler} module (see
 * {@code com.eudext.erp.scheduler.internal.SchedulingConfig}), same as
 * {@code AuditArchivalScheduler}'s.
 */
@Component
class EscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(EscalationScheduler.class);

    private final WorkflowEngine engine;
    private final EscalationProperties properties;

    EscalationScheduler(WorkflowEngine engine, EscalationProperties properties) {
        this.engine = engine;
        this.properties = properties;
    }

    @Scheduled(cron = "${eudext.workflow.escalation.cron:0 */15 * * * *}")
    void escalateDueTasks() {
        if (!properties.isEnabled() || properties.getTenantIds().isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        properties.getTenantIds().forEach(tenantId -> {
            TenantContext.set(tenantId);
            try {
                int escalated = engine.escalateDueTasks(now);
                if (escalated > 0) {
                    log.info("WF-5: escalated {} overdue approval task(s) for tenant {}", escalated, tenantId);
                }
            } catch (RuntimeException e) {
                log.error("WF-5: escalation sweep failed for tenant {}", tenantId, e);
            } finally {
                TenantContext.clear();
            }
        });
    }
}
