package com.eudext.erp.notification.internal;

/**
 * The actual delivery mechanism. Brand-aware transactional email (BRD-8:
 * per-brand sending domain, DKIM/SPF) is Epic 0.8's own scope and doesn't
 * exist yet, so the only implementation for now is {@link
 * LoggingNotificationSender} — every outbound notification is still
 * durably recorded in {@code notification_outbox} regardless, so nothing
 * here silently drops a message once a real transport is wired in later.
 */
interface NotificationSender {

    void send(String recipientEmail, String templateCode, String payloadJson);
}
