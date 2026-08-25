package com.eudext.erp.iam.internal.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * IAM-4: a user may hold different roles in different companies of the
 * same tenant. {@code companyId} is opaque until Epic 0.9's Company master
 * lands, same convention as {@code Document.companyId}.
 */
@Entity
@Table(name = "user_company_roles")
public class UserCompanyRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID roleId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserCompanyRole() {}

    public static UserCompanyRole of(UUID tenantId, UUID userId, UUID companyId, UUID roleId) {
        UserCompanyRole assignment = new UserCompanyRole();
        assignment.tenantId = tenantId;
        assignment.userId = userId;
        assignment.companyId = companyId;
        assignment.roleId = roleId;
        assignment.createdAt = Instant.now();
        return assignment;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getRoleId() {
        return roleId;
    }
}
