package com.eudext.erp.notification.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(String recipientEmail, String templateCode, String payloadJson) {
        log.info("Notification dispatched (no real transport wired yet): to={} template={}", recipientEmail, templateCode);
    }
}
