package com.eudext.erp.documents;

import java.time.Instant;
import java.util.UUID;

/** DOC-1: the subset of an attachment's metadata safe to expose outside the documents module. */
public record AttachmentRef(
        UUID id, String documentType, UUID documentId, String fileName, String contentType, long sizeBytes, Instant uploadedAt) {}
