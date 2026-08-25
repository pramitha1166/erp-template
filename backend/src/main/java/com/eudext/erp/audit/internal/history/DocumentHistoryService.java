package com.eudext.erp.audit.internal.history;

import com.eudext.erp.audit.internal.log.AuditLogEntry;
import com.eudext.erp.audit.internal.log.AuditLogRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AUD-4: read side of the audit trail — the field-level change history behind a document/record's version view. */
@Service
public class DocumentHistoryService {

    private final AuditLogRepository repository;

    public DocumentHistoryService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntry> historyOf(UUID tenantId, String entityType, String entityId) {
        return repository.findByTenantIdAndEntityTypeAndEntityIdOrderByOccurredAtAsc(tenantId, entityType, entityId);
    }
}
