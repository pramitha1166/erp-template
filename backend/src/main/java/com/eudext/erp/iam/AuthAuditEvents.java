package com.eudext.erp.iam;

import java.time.Instant;
import java.util.UUID;

/**
 * IAM-10: auth-related domain events, published via Spring Modulith's
 * event infrastructure so Epic 0.3's audit log module can subscribe with
 * {@code @ApplicationModuleListener} once it exists, without IAM depending
 * on it (ARCH-1). IAM's job here stops at publishing; persisting a queryable
 * audit trail is Epic 0.3's own scope.
 */
public final class AuthAuditEvents {

    private AuthAuditEvents() {}

    public record LoginSucceeded(UUID tenantId, UUID userId, String ipAddress, Instant occurredAt) {}

    public record LoginFailed(UUID tenantId, String email, String reason, String ipAddress, Instant occurredAt) {}

    public record SessionRevoked(UUID tenantId, UUID userId, UUID sessionId, String reason, Instant occurredAt) {}

    public record PermissionGranted(
            UUID tenantId, UUID roleId, String permissionCode, String grantedBy, Instant occurredAt) {}

    public record PermissionRevoked(
            UUID tenantId, UUID roleId, String permissionCode, String revokedBy, Instant occurredAt) {}

    public record RoleAssigned(
            UUID tenantId, UUID userId, UUID companyId, UUID roleId, String assignedBy, Instant occurredAt) {}

    public record RoleUnassigned(
            UUID tenantId, UUID userId, UUID companyId, UUID roleId, String unassignedBy, Instant occurredAt) {}
}
