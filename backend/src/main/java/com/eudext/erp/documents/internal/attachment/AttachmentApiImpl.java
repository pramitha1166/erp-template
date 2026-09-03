package com.eudext.erp.documents.internal.attachment;

import com.eudext.erp.documents.AttachmentApi;
import com.eudext.erp.documents.AttachmentRef;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class AttachmentApiImpl implements AttachmentApi {

    private final AttachmentService attachmentService;

    AttachmentApiImpl(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @Override
    public AttachmentRef upload(
            UUID tenantId,
            UUID companyId,
            String documentType,
            UUID documentId,
            String fileName,
            String contentType,
            byte[] content) {
        return toRef(attachmentService.upload(tenantId, companyId, documentType, documentId, fileName, contentType, content));
    }

    @Override
    public List<AttachmentRef> listFor(String documentType, UUID documentId) {
        return attachmentService.listFor(documentType, documentId).stream()
                .map(AttachmentApiImpl::toRef)
                .toList();
    }

    @Override
    public void delete(UUID attachmentId) {
        attachmentService.delete(attachmentId);
    }

    private static AttachmentRef toRef(Attachment attachment) {
        return new AttachmentRef(
                attachment.getId(),
                attachment.getDocumentType(),
                attachment.getDocumentId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getUploadedAt());
    }
}
