package com.eudext.erp.audit.internal.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eudext.erp.audit.internal.log.AuditLogSearchService.SearchCriteria;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AuditLogSearchServiceTest {

    @Mock
    private AuditLogRepository repository;

    @Test
    void delegatesEachCriterionToTheRepositoryQuery() {
        UUID tenantId = UUID.randomUUID();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant through = Instant.parse("2026-02-01T00:00:00Z");
        Pageable pageable = PageRequest.of(1, 25);
        Page<AuditLogEntry> page = new PageImpl<>(List.of(), pageable, 0);
        when(repository.search(tenantId, "sales.invoice", "alice@example.com", AuditAction.UPDATE, from, through, pageable))
                .thenReturn(page);
        AuditLogSearchService service = new AuditLogSearchService(repository);

        Page<AuditLogEntry> result = service.search(
                tenantId, new SearchCriteria("sales.invoice", "alice@example.com", AuditAction.UPDATE, from, through), pageable);

        assertThat(result).isSameAs(page);
        verify(repository).search(tenantId, "sales.invoice", "alice@example.com", AuditAction.UPDATE, from, through, pageable);
    }

    @Test
    void passesNullFiltersThroughUnchangedSoTheyBecomeOptionalInTheQuery() {
        UUID tenantId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.search(eq(tenantId), any(), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        AuditLogSearchService service = new AuditLogSearchService(repository);

        service.search(tenantId, new SearchCriteria(null, null, null, null, null), pageable);

        verify(repository).search(tenantId, null, null, null, null, null, pageable);
    }
}
