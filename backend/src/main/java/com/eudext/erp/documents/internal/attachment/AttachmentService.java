package com.eudext.erp.documents.internal.attachment;

import com.eudext.erp.documents.internal.DocumentType;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** DOC-1/DOC-4: uploads, lists, downloads and deletes attachments against any document type. */
@Service
public class AttachmentService {

    private final AttachmentRepository repository;
    private final Optional<AttachmentStorage> storage;
    private final Optional<VirusScanner> virusScanner;

    AttachmentService(AttachmentRepository repository, Optional<AttachmentStorage> storage, Optional<VirusScanner> virusScanner) {
        this.repository = repository;
        this.storage = storage;
        this.virusScanner = virusScanner;
    }

    /**
     * Scans (DOC-4, when configured) then stores (DOC-1) the given content. An infected upload is rejected — never
     * written to object storage or persisted here — before this method returns.
     */
    @Transactional
    public Attachment upload(
            UUID tenantId, UUID companyId, String documentType, UUID documentId, String fileName, String contentType, byte[] content) {
        DocumentType.validate(documentType);
        AttachmentStorage attachmentStorage = storage.orElseThrow(AttachmentStorageUnavailableException::new);

        ScanStatus scanStatus = ScanStatus.PENDING;
        String scanMessage = null;
        if (virusScanner.isPresent()) {
            ScanOutcome outcome = virusScanner.get().scan(content);
            if (outcome.status() == ScanStatus.INFECTED) {
                throw new InfectedFileException(fileName, outcome.message());
            }
            scanStatus = outcome.status();
            scanMessage = outcome.message();
        }

        String storageKey = buildStorageKey(tenantId, documentType, documentId, fileName);
        attachmentStorage.put(storageKey, content, contentType);

        Attachment attachment = Attachment.create(
                tenantId,
                companyId,
                documentType,
                documentId,
                fileName,
                contentType,
                content.length,
                storageKey,
                sha256(content),
                scanStatus,
                scanMessage);
        return repository.save(attachment);
    }

    @Transactional(readOnly = true)
    public List<Attachment> listFor(String documentType, UUID documentId) {
        return repository.findByDocumentTypeAndDocumentId(documentType, documentId);
    }

    @Transactional(readOnly = true)
    public Attachment get(UUID attachmentId) {
        return repository.findById(attachmentId).orElseThrow(() -> new NoSuchElementException("No such attachment"));
    }

    @Transactional(readOnly = true)
    public byte[] download(UUID attachmentId) {
        Attachment attachment = get(attachmentId);
        if (attachment.getScanStatus() == ScanStatus.INFECTED) {
            throw new InfectedFileException(attachment.getFileName(), "flagged by a previous scan");
        }
        return storage.orElseThrow(AttachmentStorageUnavailableException::new).get(attachment.getStorageKey());
    }

    @Transactional
    public void delete(UUID attachmentId) {
        Attachment attachment = get(attachmentId);
        storage.ifPresent(s -> s.delete(attachment.getStorageKey()));
        repository.delete(attachment);
    }

    private static String buildStorageKey(UUID tenantId, String documentType, UUID documentId, String fileName) {
        return tenantId + "/" + documentType.replace(':', '/') + "/" + documentId + "/" + UUID.randomUUID() + "-" + sanitize(fileName);
    }

    private static String sanitize(String fileName) {
        String sanitized = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? "file" : sanitized;
    }

    private static String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is a JDK-mandatory algorithm", e);
        }
    }
}
