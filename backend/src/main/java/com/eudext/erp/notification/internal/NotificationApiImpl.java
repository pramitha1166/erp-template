package com.eudext.erp.notification.internal;

import com.eudext.erp.notification.NotificationApi;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class NotificationApiImpl implements NotificationApi {

    private final NotificationService notificationService;

    NotificationApiImpl(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void send(UUID tenantId, String recipientEmail, String templateCode, Map<String, String> variables) {
        notificationService.send(tenantId, recipientEmail, templateCode, variables);
    }
}
