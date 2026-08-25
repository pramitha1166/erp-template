package com.eudext.erp.notification.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {}
