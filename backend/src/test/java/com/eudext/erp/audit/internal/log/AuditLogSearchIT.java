package com.eudext.erp.audit.internal.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.eudext.erp.audit.internal.write.AuditLogWriter;
import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.testsupport.AbstractIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * AUD-2 / F0.3.2: exercises {@link AuditLogRepository#search} against a real
 * Postgres — every filter is optional, so this proves both the "no filter
 * set" and "filter narrows the result" paths, plus that RLS (ARCH-2) keeps
 * one tenant's rows out of another tenant's search.
 */
class AuditLogSearchIT extends AbstractIntegrationTest {

    @Autowired
    private AuditLogRepository repository;

    @Autowired
    private AuditLogWriter writer;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void narrowsByEntityTypeActorActionAndDateRange() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);
        Instant now = Instant.now();

        write(tenantId, "sales.invoice", "INV-1", AuditAction.INSERT, "alice@example.com", now.minus(10, ChronoUnit.DAYS));
        write(tenantId, "sales.invoice", "INV-1", AuditAction.UPDATE, "alice@example.com", now.minus(5, ChronoUnit.DAYS));
        write(tenantId, "sales.invoice", "INV-2", AuditAction.UPDATE, "bob@example.com", now.minus(1, ChronoUnit.DAYS));
        write(tenantId, "inventory.item", "ITM-1", AuditAction.UPDATE, "alice@example.com", now.minus(1, ChronoUnit.DAYS));

        Page<AuditLogEntry> everything = repository.search(tenantId, null, null, null, null, null, PageRequest.of(0, 20));
        assertThat(everything.getTotalElements()).isEqualTo(4);

        Page<AuditLogEntry> byEntityType =
                repository.search(tenantId, "sales.invoice", null, null, null, null, PageRequest.of(0, 20));
        assertThat(byEntityType.getContent()).hasSize(3).allMatch(e -> e.getEntityType().equals("sales.invoice"));

        Page<AuditLogEntry> byActor = repository.search(tenantId, null, "bob@example.com", null, null, null, PageRequest.of(0, 20));
        assertThat(byActor.getContent()).hasSize(1);
        assertThat(byActor.getContent().get(0).getEntityId()).isEqualTo("INV-2");

        Page<AuditLogEntry> byAction = repository.search(tenantId, null, null, AuditAction.INSERT, null, null, PageRequest.of(0, 20));
        assertThat(byAction.getContent()).hasSize(1);
        assertThat(byAction.getContent().get(0).getEntityId()).isEqualTo("INV-1");

        Page<AuditLogEntry> byDateRange = repository.search(
                tenantId, null, null, null, now.minus(6, ChronoUnit.DAYS), now, PageRequest.of(0, 20));
        assertThat(byDateRange.getTotalElements()).isEqualTo(3);

        Page<AuditLogEntry> combined = repository.search(
                tenantId, "sales.invoice", "alice@example.com", AuditAction.UPDATE, null, null, PageRequest.of(0, 20));
        assertThat(combined.getContent()).hasSize(1);
        assertThat(combined.getContent().get(0).getEntityId()).isEqualTo("INV-1");
    }

    @Test
    void ordersNewestFirstAndPaginates() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);
        Instant now = Instant.now();
        write(tenantId, "sales.invoice", "INV-1", AuditAction.INSERT, "alice@example.com", now.minus(3, ChronoUnit.DAYS));
        write(tenantId, "sales.invoice", "INV-2", AuditAction.INSERT, "alice@example.com", now.minus(2, ChronoUnit.DAYS));
        write(tenantId, "sales.invoice", "INV-3", AuditAction.INSERT, "alice@example.com", now.minus(1, ChronoUnit.DAYS));

        Page<AuditLogEntry> firstPage = repository.search(tenantId, null, null, null, null, null, PageRequest.of(0, 2));
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent()).extracting(AuditLogEntry::getEntityId).containsExactly("INV-3", "INV-2");

        Page<AuditLogEntry> secondPage = repository.search(tenantId, null, null, null, null, null, PageRequest.of(1, 2));
        assertThat(secondPage.getContent()).extracting(AuditLogEntry::getEntityId).containsExactly("INV-1");
    }

    @Test
    void anotherTenantsRowsAreInvisibleUnderRls() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        TenantContext.set(tenantA);
        write(tenantA, "sales.invoice", "A-1", AuditAction.INSERT, "alice@example.com", Instant.now());

        TenantContext.set(tenantB);
        write(tenantB, "sales.invoice", "B-1", AuditAction.INSERT, "bob@example.com", Instant.now());

        Page<AuditLogEntry> visibleToB = repository.search(tenantA, null, null, null, null, null, PageRequest.of(0, 20));
        assertThat(visibleToB.getContent()).isEmpty();
    }

    private void write(UUID tenantId, String entityType, String entityId, AuditAction action, String actor, Instant occurredAt) {
        writer.write(tenantId, entityType, entityId, action, Map.of(), Map.of("field", "value"), actor, "127.0.0.1", "req-1", occurredAt);
    }
}
