package com.eudext.erp.documents.internal.attachment;

/** DOC-1: raised when no {@link AttachmentStorage} is configured in this environment (see {@code eudext.documents.attachments.enabled}). */
public class AttachmentStorageUnavailableException extends RuntimeException {

    public AttachmentStorageUnavailableException() {
        super("Attachment storage is not configured in this environment");
    }
}
