package com.eudext.erp.audit.internal.adminevents;

import com.eudext.erp.admin.AdminAuditEvents;
import com.eudext.erp.admin.PlatformIdentifiers;
import com.eudext.erp.audit.internal.log.AuditAction;
import com.eudext.erp.audit.internal.write.AuditLogWriter;
import com.eudext.erp.config.tenancy.TenantContextScope;
import java.util.Map;
import java.util.UUID;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * ADM-1 / ADM-6 / ADM-7 / ADM-8 / AUD-1: turns {@code admin}'s domain
 * events into {@code audit_log} rows, same pattern as {@code
 * AuthAuditEventListener} for {@code iam} — see that class's javadoc for
 * why explicit events rather than relying on the generic interceptor for
 * these specific cases.
 *
 * <p>{@code BrandCreated}/{@code BrandSuspended}/{@code TenantSuspended}/
 * etc. also get an ordinary audit_log row from the generic interceptor
 * when the {@code Brand}/{@code Tenant} entity itself is saved — that's
 * fine and expected (it captures the field-level diff); these events add a
 * second, purpose-built row for the ones ADM-7 in particular requires to
 * be unambiguously "tagged as impersonated" regardless of what the entity
 * diff shows.
 *
 * <p>Each write here explicitly enters a {@link TenantContextScope} for
 * the row's own tenant before writing it — {@code audit_log} carries RLS
 * (V10 migration) requiring the session's {@code app.tenant_id} to match
 * the row being inserted, and this listener runs after the publishing
 * transaction (and whichever tenant context was ambient there) has
 * already unwound, so it cannot assume anything about the calling
 * thread's ambient tenant. Platform-catalog events (no owning tenant, e.g.
 * a Brand's own lifecycle) are anchored to {@link
 * PlatformIdentifiers#PLATFORM_TENANT_ID} — the same sentinel platform
 * admin staff authenticate under, so their own audit log query naturally
 * includes these rows.
 */
@Component
class AdminAuditEventListener {

    private final AuditLogWriter writer;

    AdminAuditEventListener(AuditLogWriter writer) {
        this.writer = writer;
    }

    @ApplicationModuleListener
    void on(AdminAuditEvents.BrandCreated event) {
        writeAs(PlatformIdentifiers.PLATFORM_TENANT_ID, () -> writer.write(
                PlatformIdentifiers.PLATFORM_TENANT_ID,
                "Brand",
                event.brandId().toString(),
                AuditAction.INSERT,
                Map.of(),
                Map.of("event", "BRAND_CREATED"),
                event.createdBy(),
                null,
                null,
                event.occurredAt()));
    }

    @ApplicationModuleListener
    void on(AdminAuditEvents.BrandSuspended event) {
        writeAs(PlatformIdentifiers.PLATFORM_TENANT_ID, () -> writer.write(
                PlatformIdentifiers.PLATFORM_TENANT_ID,
                "Brand",
                event.brandId().toString(),
                AuditAction.UPDATE,
                Map.of(),
                Map.of("event", "BRAND_SUSPENDED", "reason", event.reason()),
                event.actor(),
                null,
                null,
                event.occurredAt()));
    }

    @ApplicationModuleListener
    void on(AdminAuditEvents.BrandReactivated event) {
        writeAs(PlatformIdentifiers.PLATFORM_TENANT_ID, () -> writer.write(
                PlatformIdentifiers.PLATFORM_TENANT_ID,
                "Brand",
                event.brandId().toString(),
                AuditAction.UPDATE,
                Map.of(),
                Map.of("event", "BRAND_REACTIVATED"),
                event.actor(),
                null,
                null,
                event.occurredAt()));
    }

    @ApplicationModuleListener
    void on(AdminAuditEvents.TenantOnboarded event) {
        writeAs(event.tenantId(), () -> writer.write(
                event.tenantId(),
                "Tenant",
                event.tenantId().toString(),
                AuditAction.INSERT,
                Map.of(),
                Map.of("event", "TENANT_ONBOARDED", "brandId", event.brandId(), "companyId", event.companyId()),
                event.actor(),
                null,
                null,
                event.occurredAt()));
    }

    @ApplicationModuleListener
    void on(AdminAuditEvents.TenantSuspended event) {
        writeAs(event.tenantId(), () -> writer.write(
                event.tenantId(),
                "Tenant",
                event.tenantId().toString(),
                AuditAction.UPDATE,
                Map.of(),
                Map.of("event", "TENANT_SUSPENDED", "reason", event.reason()),
                event.actor(),
                null,
                null,
                event.occurredAt()));
    }

    @ApplicationModuleListener
    void on(AdminAuditEvents.TenantReactivated event) {
        writeAs(event.tenantId(), () -> writer.write(
                event.tenantId(),
                "Tenant",
                event.tenantId().toString(),
                AuditAction.UPDATE,
                Map.of(),
                Map.of("event", "TENANT_REACTIVATED"),
                event.actor(),
                null,
                null,
                event.occurredAt()));
    }

    @ApplicationModuleListener
    void on(AdminAuditEvents.ImpersonationStarted event) {
        writeAs(event.tenantId(), () -> writer.write(
                event.tenantId(),
                "ImpersonationSession",
                event.sessionId().toString(),
                AuditAction.INSERT,
                Map.of(),
                Map.of(
                        "event", "IMPERSONATION_STARTED",
                        "impersonated", true,
                        "actorUserId", event.actorUserId(),
                        "targetUserId", event.targetUserId(),
                        "reason", event.reason(),
                        "expiresAt", event.expiresAt().toString()),
                event.actorUserId().toString(),
                null,
                null,
                event.occurredAt()));
    }

    @ApplicationModuleListener
    void on(AdminAuditEvents.ImpersonationEnded event) {
        writeAs(event.tenantId(), () -> writer.write(
                event.tenantId(),
                "ImpersonationSession",
                event.sessionId().toString(),
                AuditAction.UPDATE,
                Map.of(),
                Map.of("event", "IMPERSONATION_ENDED", "impersonated", true, "actorUserId", event.actorUserId()),
                event.actorUserId().toString(),
                null,
                null,
                event.occurredAt()));
    }

    @ApplicationModuleListener
    void on(AdminAuditEvents.DataSubjectRequestCreated event) {
        writeAs(event.tenantId(), () -> writer.write(
                event.tenantId(),
                "DataSubjectRequest",
                event.requestId().toString(),
                AuditAction.INSERT,
                Map.of(),
                Map.of("event", "DATA_SUBJECT_REQUEST_CREATED", "type", event.type()),
                event.requestedBy(),
                null,
                null,
                event.occurredAt()));
    }

    @ApplicationModuleListener
    void on(AdminAuditEvents.DataSubjectRequestCompleted event) {
        writeAs(event.tenantId(), () -> writer.write(
                event.tenantId(),
                "DataSubjectRequest",
                event.requestId().toString(),
                AuditAction.UPDATE,
                Map.of(),
                Map.of("event", "DATA_SUBJECT_REQUEST_COMPLETED", "type", event.type()),
                "system",
                null,
                null,
                event.occurredAt()));
    }

    private void writeAs(UUID tenantId, Runnable write) {
        try (var scope = TenantContextScope.enter(tenantId)) {
            write.run();
        }
    }
}
