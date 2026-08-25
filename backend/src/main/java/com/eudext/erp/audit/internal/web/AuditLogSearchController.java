package com.eudext.erp.audit.internal.web;

import com.eudext.erp.audit.internal.log.AuditAction;
import com.eudext.erp.audit.internal.log.AuditLogEntry;
import com.eudext.erp.audit.internal.log.AuditLogSearchService;
import com.eudext.erp.audit.internal.log.AuditLogSearchService.SearchCriteria;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** AUD-2: read-only API behind the admin audit log browser/search screen (F0.3.2). */
@RestController
@RequestMapping("/audit/log")
public class AuditLogSearchController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogSearchService searchService;
    private final AuditAccessGuard accessGuard;

    public AuditLogSearchController(AuditLogSearchService searchService, AuditAccessGuard accessGuard) {
        this.searchService = searchService;
        this.accessGuard = accessGuard;
    }

    public record AuditLogEntryView(
            UUID id,
            String entityType,
            String entityId,
            AuditAction action,
            String actor,
            Instant occurredAt,
            Map<String, Object> oldValues,
            Map<String, Object> newValues) {}

    public record AuditLogPageView(List<AuditLogEntryView> content, long totalElements, int page, int size) {}

    @GetMapping
    public AuditLogPageView search(
            @RequestParam UUID companyId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant through,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        accessGuard.requirePermission(companyId);
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<AuditLogEntry> result = searchService.search(
                accessGuard.tenantId(),
                new SearchCriteria(entityType, actor, action, from, through),
                PageRequest.of(Math.max(page, 0), boundedSize));
        return new AuditLogPageView(
                result.getContent().stream().map(AuditLogSearchController::toView).toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize());
    }

    private static AuditLogEntryView toView(AuditLogEntry entry) {
        return new AuditLogEntryView(
                entry.getId(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getAction(),
                entry.getActor(),
                entry.getOccurredAt(),
                entry.getOldValues(),
                entry.getNewValues());
    }
}
