package com.eudext.erp.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * ADM-1 / ADM-6 / ADM-7 / ADM-8: admin-domain events that don't correspond
 * to a single entity mutation the generic {@code AuditingInterceptor} would
 * see cleanly (or that need an explicit, human-readable audit trail entry
 * regardless), published so the {@code audit} module's own listener can
 * turn them into {@code audit_log} rows — same pattern as {@code
 * iam.AuthAuditEvents}, see its javadoc.
 */
public final class AdminAuditEvents {

    private AdminAuditEvents() {}

    public record BrandCreated(UUID brandId, String createdBy, Instant occurredAt) {}

    public record BrandSuspended(UUID brandId, String reason, String actor, Instant occurredAt) {}

    public record BrandReactivated(UUID brandId, String actor, Instant occurredAt) {}

    public record TenantOnboarded(UUID tenantId, UUID brandId, UUID companyId, String actor, Instant occurredAt) {}

    public record TenantSuspended(UUID tenantId, String reason, String actor, Instant occurredAt) {}

    public record TenantReactivated(UUID tenantId, String actor, Instant occurredAt) {}

    /** ADM-7: the explicit "tagged as impersonated" audit entry required alongside per-mutation tagging (see {@code ImpersonationContext}). */
    public record ImpersonationStarted(
            UUID sessionId, UUID tenantId, UUID actorUserId, UUID targetUserId, String reason, Instant expiresAt, Instant occurredAt) {}

    public record ImpersonationEnded(UUID sessionId, UUID tenantId, UUID actorUserId, UUID targetUserId, Instant occurredAt) {}

    public record DataSubjectRequestCreated(UUID requestId, UUID tenantId, String type, String requestedBy, Instant occurredAt) {}

    public record DataSubjectRequestCompleted(UUID requestId, UUID tenantId, String type, Instant occurredAt) {}
}
