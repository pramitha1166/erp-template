package com.eudext.erp.iam.internal.recordscope;

import com.eudext.erp.iam.RecordScopeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** IAM-6: one permitted scope value for a role on a warehouse/cost-centre/branch dimension. */
@Entity
@Table(name = "record_scope_restrictions")
public class RecordScopeRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, updatable = false)
    private RecordScopeType scopeType;

    @Column(name = "scope_value", nullable = false, updatable = false)
    private UUID scopeValue;

    protected RecordScopeRestriction() {}

    public static RecordScopeRestriction of(UUID tenantId, UUID roleId, RecordScopeType scopeType, UUID scopeValue) {
        RecordScopeRestriction restriction = new RecordScopeRestriction();
        restriction.tenantId = tenantId;
        restriction.roleId = roleId;
        restriction.scopeType = scopeType;
        restriction.scopeValue = scopeValue;
        return restriction;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public RecordScopeType getScopeType() {
        return scopeType;
    }

    public UUID getScopeValue() {
        return scopeValue;
    }
}
