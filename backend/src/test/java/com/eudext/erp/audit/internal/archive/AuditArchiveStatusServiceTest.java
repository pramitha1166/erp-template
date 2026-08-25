package com.eudext.erp.audit.internal.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.Period;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditArchiveStatusServiceTest {

    @Mock
    private AuditArchiveWatermarkRepository watermarkRepository;

    private final UUID tenantId = UUID.randomUUID();

    private AuditArchiveProperties properties(boolean enabled, Period retentionBeforeArchive) {
        AuditArchiveProperties properties = new AuditArchiveProperties();
        properties.setEnabled(enabled);
        properties.setRetentionBeforeArchive(retentionBeforeArchive);
        return properties;
    }

    @Test
    void reportsNoArchivalYetWhenTheTenantHasNoWatermarkRow() {
        when(watermarkRepository.findById(tenantId)).thenReturn(Optional.empty());
        AuditArchiveStatusService service = new AuditArchiveStatusService(watermarkRepository, properties(true, Period.ofYears(2)));

        AuditArchiveStatusService.ArchiveStatus status = service.statusFor(tenantId);

        assertThat(status.archivalEnabled()).isTrue();
        assertThat(status.archivedThrough()).isNull();
        assertThat(status.lastObjectKey()).isNull();
        assertThat(status.coldStorageAfterYears()).isEqualTo(2);
        assertThat(status.minimumRetentionYears()).isEqualTo(7);
    }

    @Test
    void reportsTheWatermarkPositionWhenArchivalHasRun() {
        Instant archivedThrough = Instant.parse("2024-06-01T00:00:00Z");
        AuditArchiveWatermark watermark = AuditArchiveWatermark.initial(tenantId, archivedThrough);
        watermark.advanceTo(archivedThrough, "tenant/object-key.json");
        when(watermarkRepository.findById(tenantId)).thenReturn(Optional.of(watermark));
        AuditArchiveStatusService service = new AuditArchiveStatusService(watermarkRepository, properties(true, Period.ofYears(2)));

        AuditArchiveStatusService.ArchiveStatus status = service.statusFor(tenantId);

        assertThat(status.archivedThrough()).isEqualTo(archivedThrough);
        assertThat(status.lastObjectKey()).isEqualTo("tenant/object-key.json");
    }

    @Test
    void reflectsArchivalBeingDisabledIndependentlyOfAnyWatermark() {
        when(watermarkRepository.findById(tenantId)).thenReturn(Optional.empty());
        AuditArchiveStatusService service = new AuditArchiveStatusService(watermarkRepository, properties(false, Period.ofYears(2)));

        assertThat(service.statusFor(tenantId).archivalEnabled()).isFalse();
    }
}
