package com.eudext.erp.iam.internal.password;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** IAM-9: insert-only — a user's past password hashes, checked to block reuse. Never updated or pruned. */
@Entity
@Table(name = "password_history")
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
