package com.eudext.erp.notification;

import java.util.Map;
import java.util.UUID;

/**
 * ADM-5 / ADM-7: sends a tenant-admin invite, a suspension/impersonation
 * notice, or similar templated notification. {@code tenantId} may be
 * {@code null} for a platform-level notice with no owning tenant.
 */
public interface NotificationApi {

    void send(UUID tenantId, String recipientEmail, String templateCode, Map<String, String> variables);
}
