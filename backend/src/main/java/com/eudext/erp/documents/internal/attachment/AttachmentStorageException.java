package com.eudext.erp.documents.internal.attachment;

/** Wraps a lower-level object-storage failure (I/O, connectivity) behind a module-owned unchecked exception. */
class AttachmentStorageException extends RuntimeException {

    AttachmentStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
