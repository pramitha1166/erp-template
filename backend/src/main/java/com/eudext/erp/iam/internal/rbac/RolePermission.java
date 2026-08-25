package com.eudext.erp.iam.internal.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** IAM-3: a single `module:entity:action` grant on a {@link Role}. */
@Entity
@Table(name = "role_permissions")
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID roleId;

    @Column(name = "permission_code", nullable = false, updatable = false)
    private String permissionCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RolePermission() {}

    public static RolePermission of(UUID tenantId, UUID roleId, String permissionCode) {
        RolePermission grant = new RolePermission();
        grant.tenantId = tenantId;
        grant.roleId = roleId;
        grant.permissionCode = permissionCode;
        grant.createdAt = Instant.now();
        return grant;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public String getPermissionCode() {
        return permissionCode;
    }
}
