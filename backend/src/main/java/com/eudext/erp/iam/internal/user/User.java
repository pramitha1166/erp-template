package com.eudext.erp.iam.internal.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * IAM-1: a tenant's user account. Not a {@link com.eudext.erp.config.document.Document}
 * subtype — users aren't transactional business documents, so ARCH-3/ARCH-4
 * don't apply, but ARCH-2 (tenant_id + RLS) and ARCH-6 (optimistic locking)
 * still do.
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "email", nullable = false, updatable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "password_algo", nullable = false)
    private String passwordAlgo = "ARGON2ID";

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "totp_enabled", nullable = false)
    private boolean totpEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

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

    protected User() {}

    public static User create(UUID tenantId, String email, String passwordHash) {
        User user = new User();
        user.tenantId = tenantId;
        user.email = email;
        user.passwordHash = passwordHash;
        user.passwordChangedAt = Instant.now();
        return user;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordChangedAt = Instant.now();
    }

    public String getPasswordAlgo() {
        return passwordAlgo;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    /** IAM-2: stores a candidate secret without turning 2FA on yet — {@link #confirmTotpEnrollment()} does that. */
    public void beginTotpEnrollment(String secret) {
        this.totpSecret = secret;
        this.totpEnabled = false;
    }

    /** IAM-2: flips 2FA on once the caller has verified a code against the pending secret. */
    public void confirmTotpEnrollment() {
        if (totpSecret == null) {
            throw new IllegalStateException("No pending TOTP enrollment to confirm");
        }
        this.totpEnabled = true;
    }

    public void disableTotp() {
        this.totpSecret = null;
        this.totpEnabled = false;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void disable() {
        this.status = UserStatus.DISABLED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void recordFailedLogin(int maxAttempts, java.time.Duration lockoutDuration) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = Instant.now().plus(lockoutDuration);
        }
    }

    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public long getVersion() {
        return version;
    }
}
