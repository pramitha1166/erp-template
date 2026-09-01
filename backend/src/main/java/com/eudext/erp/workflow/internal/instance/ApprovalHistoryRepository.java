package com.eudext.erp.workflow.internal.instance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** No update/delete method is ever declared here — append-only (WF-7), same convention as {@code AuditLogRepository}. */
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistoryEntry, UUID> {

    List<ApprovalHistoryEntry> findByInstanceIdOrderByOccurredAtAsc(UUID instanceId);
}
