package com.eudext.erp.documents.internal.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** DOC-1/DOC-4: upload/list/download/delete, with virus scanning wired into the upload path. */
@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository repository;

    private final Map<String, byte[]> fakeBucket = new HashMap<>();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();

    private AttachmentStorage fakeStorage() {
        return new AttachmentStorage() {
            @Override
            public void put(String objectKey, byte[] content, String contentType) {
                fakeBucket.put(objectKey, content);
            }

            @Override
            public byte[] get(String objectKey) {
                return fakeBucket.get(objectKey);
            }

            @Override
            public void delete(String objectKey) {
                fakeBucket.remove(objectKey);
            }
        };
    }

    @Test
    void uploadStoresContentAndPersistsMetadataWhenScanIsClean() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AttachmentService service = new AttachmentService(repository, Optional.of(fakeStorage()), Optional.of(content -> ScanOutcome.clean()));

        Attachment attachment = service.upload(
                tenantId, companyId, "sales:invoice", documentId, "invoice.pdf", "application/pdf", "hello".getBytes());

        assertThat(attachment.getScanStatus()).isEqualTo(ScanStatus.CLEAN);
        assertThat(attachment.getSizeBytes()).isEqualTo(5);
        assertThat(attachment.getChecksumSha256()).hasSize(64);
        assertThat(fakeBucket).containsKey(attachment.getStorageKey());
        verify(repository).save(any());
    }

    @Test
    void uploadMarksPendingWhenNoScannerConfigured() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AttachmentService service = new AttachmentService(repository, Optional.of(fakeStorage()), Optional.empty());

        Attachment attachment = service.upload(
                tenantId, companyId, "sales:invoice", documentId, "invoice.pdf", "application/pdf", "hello".getBytes());

        assertThat(attachment.getScanStatus()).isEqualTo(ScanStatus.PENDING);
    }

    @Test
    void uploadRejectsInfectedContentAndNeverStoresOrPersistsIt() {
        AttachmentService service = new AttachmentService(
                repository, Optional.of(fakeStorage()), Optional.of(content -> ScanOutcome.infected("Eicar-Test-Signature")));

        assertThatThrownBy(() -> service.upload(
                        tenantId, companyId, "sales:invoice", documentId, "eicar.txt", "text/plain", "x".getBytes()))
                .isInstanceOf(InfectedFileException.class);

        assertThat(fakeBucket).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void uploadFailsFastWhenNoStorageConfigured() {
        AttachmentService service = new AttachmentService(repository, Optional.empty(), Optional.empty());

        assertThatThrownBy(() -> service.upload(
                        tenantId, companyId, "sales:invoice", documentId, "invoice.pdf", "application/pdf", "hello".getBytes()))
                .isInstanceOf(AttachmentStorageUnavailableException.class);
    }

    @Test
    void uploadRejectsAMalformedDocumentType() {
        AttachmentService service = new AttachmentService(repository, Optional.of(fakeStorage()), Optional.empty());

        assertThatThrownBy(() -> service.upload(
                        tenantId, companyId, "SalesInvoice", documentId, "invoice.pdf", "application/pdf", "hello".getBytes()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listForDelegatesToTheRepository() {
        AttachmentService service = new AttachmentService(repository, Optional.of(fakeStorage()), Optional.empty());
        when(repository.findByDocumentTypeAndDocumentId("sales:invoice", documentId)).thenReturn(List.of());

        assertThat(service.listFor("sales:invoice", documentId)).isEmpty();
    }

    @Test
    void deleteRemovesBothTheObjectAndTheMetadataRow() {
        AttachmentStorage storage = mock(AttachmentStorage.class);
        AttachmentService service = new AttachmentService(repository, Optional.of(storage), Optional.empty());
        Attachment attachment = Attachment.create(
                tenantId, companyId, "sales:invoice", documentId, "invoice.pdf", "application/pdf", 5, "key", "checksum", ScanStatus.CLEAN, null);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(attachment));

        service.delete(id);

        verify(storage, times(1)).delete("key");
        verify(repository).delete(attachment);
    }
}
