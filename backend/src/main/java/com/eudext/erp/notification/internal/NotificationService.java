package com.eudext.erp.notification.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationOutboxRepository repository;
    private final NotificationSender sender;
    private final ObjectMapper objectMapper;

    NotificationService(NotificationOutboxRepository repository, NotificationSender sender, ObjectMapper objectMapper) {
        this.repository = repository;
        this.sender = sender;
        this.objectMapper = objectMapper;
    }

    @Transactional
    void send(UUID tenantId, String recipientEmail, String templateCode, Map<String, String> variables) {
        String payloadJson = toJson(variables);
        NotificationOutbox entry = repository.save(NotificationOutbox.create(tenantId, recipientEmail, templateCode, payloadJson));
        try {
            sender.send(recipientEmail, templateCode, payloadJson);
            entry.markSent();
        } catch (RuntimeException e) {
            log.error("Notification send failed: template={} recipient={}", templateCode, recipientEmail, e);
            entry.markFailed(e.getMessage());
        }
        repository.save(entry);
    }

    private String toJson(Map<String, String> variables) {
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize notification payload", e);
        }
    }
}
