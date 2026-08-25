package com.eudext.erp.iam.internal.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * IAM-8 / IAM-9: one row per tenant, only once the tenant has customized
 * its defaults away from {@link SecurityPolicy#defaults()} — see the V6
 * migration comment on why absence of a row is meaningful (not an error).
 */
@Entity
@Table(name = "tenant_security_settings")
@EntityListeners(AuditingEntityListener.class)
public class TenantSecuritySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "idle_timeout_minutes", nullable = false)
    private int idleTimeoutMinutes;

    @Column(name = "password_min_length", nullable = false)
    private int passwordMinLength;

    @Column(name = "password_require_upper", nullable = false)
    private boolean passwordRequireUpper;

    @Column(name = "password_require_lower", nullable = false)
    private boolean passwordRequireLower;

    @Column(name = "password_require_digit", nullable = false)
    private boolean passwordRequireDigit;

    @Column(name = "password_require_symbol", nullable = false)
    private boolean passwordRequireSymbol;

    @Column(name = "password_history_count", nullable = false)
    private int passwordHistoryCount;

    @Column(name = "password_expiry_days")
    private Integer passwordExpiryDays;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedBy
    @Column(name = "modified_by")
    private String modifiedBy;

    @LastModifiedDate
    @Column(name = "modified_at")
    private Instant modifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected TenantSecuritySettings() {}

    public static TenantSecuritySettings create(UUID tenantId, SecurityPolicy policy) {
        TenantSecuritySettings settings = new TenantSecuritySettings();
        settings.tenantId = tenantId;
        settings.applyFrom(policy);
        return settings;
    }

    public void applyFrom(SecurityPolicy policy) {
        this.idleTimeoutMinutes = policy.idleTimeoutMinutes();
        this.passwordMinLength = policy.minLength();
        this.passwordRequireUpper = policy.requireUpper();
        this.passwordRequireLower = policy.requireLower();
        this.passwordRequireDigit = policy.requireDigit();
        this.passwordRequireSymbol = policy.requireSymbol();
        this.passwordHistoryCount = policy.historyCount();
        this.passwordExpiryDays = policy.expiryDays();
    }

    public SecurityPolicy toPolicy() {
        return new SecurityPolicy(
                idleTimeoutMinutes,
                passwordMinLength,
                passwordRequireUpper,
                passwordRequireLower,
                passwordRequireDigit,
                passwordRequireSymbol,
                passwordHistoryCount,
                passwordExpiryDays);
    }

    public UUID getTenantId() {
        return tenantId;
    }
}
