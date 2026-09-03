package com.eudext.erp.documents.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.documents.internal.attachment.Attachment;
import com.eudext.erp.documents.internal.attachment.AttachmentService;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** DOC-1/DOC-4/DOC-5: generic file attachments on any document, permission-checked against that document's own permission codes. */
@RestController
@RequestMapping("/documents/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final DocumentsAccessControl accessControl;

    public AttachmentController(AttachmentService attachmentService, DocumentsAccessControl accessControl) {
        this.attachmentService = attachmentService;
        this.accessControl = accessControl;
    }

    public record AttachmentView(
            UUID id,
            String documentType,
            UUID documentId,
            String fileName,
            String contentType,
            long sizeBytes,
            String checksumSha256,
            String scanStatus,
            String uploadedBy,
            Instant uploadedAt) {
        static AttachmentView from(Attachment attachment) {
            return new AttachmentView(
                    attachment.getId(),
                    attachment.getDocumentType(),
                    attachment.getDocumentId(),
                    attachment.getFileName(),
                    attachment.getContentType(),
                    attachment.getSizeBytes(),
                    attachment.getChecksumSha256(),
                    attachment.getScanStatus().name(),
                    attachment.getUploadedBy(),
                    attachment.getUploadedAt());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentView upload(
            @RequestParam UUID companyId,
            @RequestParam String documentType,
            @RequestParam UUID documentId,
            @RequestParam("file") MultipartFile file)
            throws IOException {
        accessControl.requireDocumentPermission(companyId, documentType, "manage");
        String contentType = file.getContentType() == null || file.getContentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : file.getContentType();
        Attachment attachment = attachmentService.upload(
                tenantId(), companyId, documentType, documentId, file.getOriginalFilename(), contentType, file.getBytes());
        return AttachmentView.from(attachment);
    }

    @GetMapping
    public List<AttachmentView> list(
            @RequestParam UUID companyId, @RequestParam String documentType, @RequestParam UUID documentId) {
        accessControl.requireDocumentPermission(companyId, documentType, "view");
        return attachmentService.listFor(documentType, documentId).stream()
                .map(AttachmentView::from)
                .toList();
    }

    @GetMapping("/{attachmentId}")
    public AttachmentView get(@PathVariable UUID attachmentId) {
        Attachment attachment = attachmentService.get(attachmentId);
        accessControl.requireDocumentPermission(attachment.getCompanyId(), attachment.getDocumentType(), "view");
        return AttachmentView.from(attachment);
    }

    @GetMapping("/{attachmentId}/content")
    public ResponseEntity<byte[]> download(@PathVariable UUID attachmentId) {
        Attachment attachment = attachmentService.get(attachmentId);
        accessControl.requireDocumentPermission(attachment.getCompanyId(), attachment.getDocumentType(), "view");
        byte[] content = attachmentService.download(attachmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(content);
    }

    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID attachmentId) {
        Attachment attachment = attachmentService.get(attachmentId);
        accessControl.requireDocumentPermission(attachment.getCompanyId(), attachment.getDocumentType(), "manage");
        attachmentService.delete(attachmentId);
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
