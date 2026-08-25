package com.eudext.erp.audit.internal.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eudext.erp.audit.internal.log.AuditLogEntry;
import com.eudext.erp.audit.internal.log.AuditLogRepository;
import com.eudext.erp.config.tenancy.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditArchiveServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditArchiveWatermarkRepository watermarkRepository;

    @Mock
    private AuditArchiveStorage storage;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final UUID tenantId = UUID.randomUUID();

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    private AuditArchiveProperties properties() {
        AuditArchiveProperties properties = new AuditArchiveProperties();
        properties.setRetentionBeforeArchive(Period.ofYears(2));
        return properties;
    }

    @Test
    void skipsArchivalWhenNoStorageIsConfigured() {
        AuditArchiveService service =
                new AuditArchiveService(auditLogRepository, watermarkRepository, properties(), Optional.empty(), objectMapper);

        AuditArchiveService.ArchiveResult result = service.archiveForTenant(tenantId);

        assertThat(result.rowsArchived()).isZero();
        verify(watermarkRepository, never()).findById(any());
    }

    @Test
    void archivesNothingWhenTheWindowIsEmpty() {
        when(watermarkRepository.findById(tenantId)).thenReturn(Optional.empty());
        when(auditLogRepository.findByTenantIdAndOccurredAtAfterAndOccurredAtLessThanEqualOrderByOccurredAtAsc(
                        any(), any(), any()))
                .thenReturn(List.of());
        AuditArchiveService service =
                new AuditArchiveService(auditLogRepository, watermarkRepository, properties(), Optional.of(storage), objectMapper);

        AuditArchiveService.ArchiveResult result = service.archiveForTenant(tenantId);

        assertThat(result.rowsArchived()).isZero();
        verify(storage, never()).put(anyString(), any());
        verify(watermarkRepository, never()).save(any());
    }

    @Test
    void archivesTheBatchAndAdvancesTheWatermarkToItsLastEntry() {
        Instant older = Instant.now().minus(Duration.ofDays(365 * 3));
        Instant newer = older.plus(1, ChronoUnit.DAYS);
        AuditLogEntry first = entryOccurringAt(older);
        AuditLogEntry second = entryOccurringAt(newer);

        when(watermarkRepository.findById(tenantId)).thenReturn(Optional.empty());
        when(auditLogRepository.findByTenantIdAndOccurredAtAfterAndOccurredAtLessThanEqualOrderByOccurredAtAsc(
                        any(), any(), any()))
                .thenReturn(List.of(first, second));
        AuditArchiveService service =
                new AuditArchiveService(auditLogRepository, watermarkRepository, properties(), Optional.of(storage), objectMapper);

        AuditArchiveService.ArchiveResult result = service.archiveForTenant(tenantId);

        assertThat(result.rowsArchived()).isEqualTo(2);
        verify(storage).put(anyString(), any());

        ArgumentCaptor<AuditArchiveWatermark> captor = ArgumentCaptor.forClass(AuditArchiveWatermark.class);
        verify(watermarkRepository).save(captor.capture());
        assertThat(captor.getValue().getArchivedThrough()).isEqualTo(newer);
    }

    @Test
    void resumesFromAnExistingWatermarkRatherThanReArchivingEverything() {
        Instant watermarkPosition = Instant.now().minus(Duration.ofDays(365 * 3));
        AuditArchiveWatermark existing = AuditArchiveWatermark.initial(tenantId, watermarkPosition);
        when(watermarkRepository.findById(tenantId)).thenReturn(Optional.of(existing));
        when(auditLogRepository.findByTenantIdAndOccurredAtAfterAndOccurredAtLessThanEqualOrderByOccurredAtAsc(
                        any(), any(), any()))
                .thenReturn(List.of());
        AuditArchiveService service =
                new AuditArchiveService(auditLogRepository, watermarkRepository, properties(), Optional.of(storage), objectMapper);

        service.archiveForTenant(tenantId);

        verify(auditLogRepository)
                .findByTenantIdAndOccurredAtAfterAndOccurredAtLessThanEqualOrderByOccurredAtAsc(
                        eq(tenantId), eq(watermarkPosition), any());
    }

    @Test
    void clearsTenantContextEvenWhenStorageFails() {
        AuditLogEntry entry = entryOccurringAt(Instant.now().minus(Duration.ofDays(365 * 3)));
        when(watermarkRepository.findById(tenantId)).thenReturn(Optional.empty());
        when(auditLogRepository.findByTenantIdAndOccurredAtAfterAndOccurredAtLessThanEqualOrderByOccurredAtAsc(
                        any(), any(), any()))
                .thenReturn(List.of(entry));
        RuntimeException failure = new RuntimeException("S3 unreachable");
        Mockito.doThrow(failure).when(storage).put(anyString(), any());
        AuditArchiveService service =
                new AuditArchiveService(auditLogRepository, watermarkRepository, properties(), Optional.of(storage), objectMapper);

        assertThatThrownBy(() -> service.archiveForTenant(tenantId)).isSameAs(failure);

        assertThat(TenantContext.get()).isEmpty();
    }

    private static AuditLogEntry entryOccurringAt(Instant occurredAt) {
        AuditLogEntry entry = mock(AuditLogEntry.class);
        when(entry.getOccurredAt()).thenReturn(occurredAt);
        return entry;
    }
}
