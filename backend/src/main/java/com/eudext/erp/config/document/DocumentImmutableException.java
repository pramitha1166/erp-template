package com.eudext.erp.config.document;

import java.util.UUID;

/**
 * Thrown by {@link DocumentImmutabilityGuard} when an update attempts to
 * change a field on a document that ARCH-4 requires to stay frozen —
 * anything past {@code DRAFT} other than a field marked {@link Amendable}.
 */
public class DocumentImmutableException extends RuntimeException {

    public DocumentImmutableException(UUID documentId, String fieldName) {
        super("Document " + documentId + " is immutable: field '" + fieldName
                + "' is not marked @Amendable and cannot change once submitted");
    }
}
