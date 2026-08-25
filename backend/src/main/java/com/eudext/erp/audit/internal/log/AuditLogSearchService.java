package com.eudext.erp.audit.internal.log;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AUD-2: read side of the audit log browser — filtered, paginated search across all entities for a tenant. */
@Service
public class AuditLogSearchService {

    private final AuditLogRepository repository;

    public AuditLogSearchService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public record SearchCriteria(String entityType, String actor, AuditAction action, Instant from, Instant through) {}

    @Transactional(readOnly = true)
    public Page<AuditLogEntry> search(UUID tenantId, SearchCriteria criteria, Pageable pageable) {
        return repository.search(
                tenantId, criteria.entityType(), criteria.actor(), criteria.action(), criteria.from(), criteria.through(), pageable);
    }
}
