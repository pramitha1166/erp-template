package com.eudext.erp.iam.internal.fieldperm;

import com.eudext.erp.iam.FieldAccess;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** IAM-5: the access a {@link com.eudext.erp.iam.internal.rbac.Role} has on one field of one entity. */
@Entity
@Table(name = "field_permissions")
public class FieldPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID roleId;

    @Column(name = "entity_code", nullable = false, updatable = false)
    private String entityCode;

    @Column(name = "field_name", nullable = false, updatable = false)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "access", nullable = false)
    private FieldAccess access;

    protected FieldPermission() {}

    public static FieldPermission of(UUID tenantId, UUID roleId, String entityCode, String fieldName, FieldAccess access) {
        FieldPermission permission = new FieldPermission();
        permission.tenantId = tenantId;
        permission.roleId = roleId;
        permission.entityCode = entityCode;
        permission.fieldName = fieldName;
        permission.access = access;
        return permission;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public String getEntityCode() {
        return entityCode;
    }

    public String getFieldName() {
        return fieldName;
    }

    public FieldAccess getAccess() {
        return access;
    }

    public void setAccess(FieldAccess access) {
        this.access = access;
    }
}
