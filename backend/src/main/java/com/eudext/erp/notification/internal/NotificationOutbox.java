package com.eudext.erp.notification.internal;

import com.eudext.erp.config.audit.NotAudited;
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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * ADM-7 / ADM-5: an outbound notification record. {@code tenantId} is
 * nullable because platform-level notices (e.g. to Eudext operators) have
 * no owning tenant; see the V16 migration for how that's reconciled with
 * RLS. {@code @NotAudited}: this table already is its own send-state
 * record — running it back through the generic audit interceptor as well
 * would double-log every send with no benefit.
 */
@Entity
@Table(name = "notification_outbox")
@EntityListeners(AuditingEntityListener.class)
@NotAudited
public class NotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", updatable = false)
    private UUID tenantId;

    @Column(name = "recipient_email", nullable = false, updatable = false)
    private String recipientEmail;

    @Column(name = "template_code", nullable = false, updatable = false)
    private String templateCode;

    @Column(name = "payload", columnDefinition = "jsonb", updatable = false)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected NotificationOutbox() {}

    public static NotificationOutbox create(UUID tenantId, String recipientEmail, String templateCode, String payloadJson) {
        NotificationOutbox entry = new NotificationOutbox();
        entry.tenantId = tenantId;
        entry.recipientEmail = recipientEmail;
        entry.templateCode = templateCode;
        entry.payloadJson = payloadJson;
        entry.createdAt = Instant.now();
        return entry;
    }

    public UUID getId() {
        return id;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
    }
}
