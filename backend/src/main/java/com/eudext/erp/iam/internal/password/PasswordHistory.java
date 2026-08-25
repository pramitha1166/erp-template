package com.eudext.erp.iam.internal.password;

import com.eudext.erp.config.audit.NotAudited;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * IAM-9: insert-only — a user's past password hashes, checked to block
 * reuse. Never updated or pruned. {@link NotAudited}: it is itself
 * effectively an append-only audit record of password changes (AUD-1
 * already covers {@code User.passwordHash} changes at the field level, via
 * {@link com.eudext.erp.config.audit.AuditRedacted}), and generically
 * auditing it would additionally write the very hash material AUD-2
 * requires keeping out of the audit trail.
 */
@Entity
@Table(name = "password_history")
@NotAudited
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "password_hash", nullable = false, updatable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PasswordHistory() {}

    public static PasswordHistory of(UUID tenantId, UUID userId, String passwordHash) {
        PasswordHistory history = new PasswordHistory();
        history.tenantId = tenantId;
        history.userId = userId;
        history.passwordHash = passwordHash;
        history.createdAt = Instant.now();
        return history;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
