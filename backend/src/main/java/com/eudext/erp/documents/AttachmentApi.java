package com.eudext.erp.documents;

import java.util.List;
import java.util.UUID;

/**
 * DOC-1: the entry point other modules use to attach files to their own documents without reaching into the
 * documents module's internal tables (ARCH-1). REST callers (the frontend) go through {@code
 * com.eudext.erp.documents.internal.web.AttachmentController} instead — this exists for backend-to-backend use,
 * e.g. a future module auto-attaching a generated document.
 */
public interface AttachmentApi {

    AttachmentRef upload(
            UUID tenantId,
            UUID companyId,
            String documentType,
            UUID documentId,
            String fileName,
            String contentType,
            byte[] content);

    List<AttachmentRef> listFor(String documentType, UUID documentId);

    void delete(UUID attachmentId);
}
