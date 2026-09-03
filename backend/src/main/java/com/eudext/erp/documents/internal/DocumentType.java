package com.eudext.erp.documents.internal;

import java.util.regex.Pattern;

/**
 * DOC-5: the {@code module:entity} pair identifying which owning module and entity a generic document reference (an
 * attachment's or print format's {@code documentType}) belongs to — the same shape as the first two segments of an
 * IAM {@code PermissionCode}. Deriving {@code documentType + ":" + action} lets this module ask IAM's
 * {@code PermissionApi} whether a user may act on the parent document without ever reading the owning module's own
 * tables (ARCH-1 forbids that): an attachment on a {@code sales:invoice} is only visible/manageable to whoever
 * already holds {@code sales:invoice:view} / {@code sales:invoice:manage}.
 */
public final class DocumentType {

    private static final Pattern SEGMENT = Pattern.compile("[a-z][a-z0-9-]*");

    private DocumentType() {}

    public static void validate(String documentType) {
        if (documentType == null) {
            throw new IllegalArgumentException("documentType is required");
        }
        String[] parts = documentType.split(":", -1);
        if (parts.length != 2 || !SEGMENT.matcher(parts[0]).matches() || !SEGMENT.matcher(parts[1]).matches()) {
            throw new IllegalArgumentException(
                    "documentType must be `module:entity` (matching " + SEGMENT.pattern() + "), got: " + documentType);
        }
    }

    /** {@code documentType + ":" + action}, e.g. {@code sales:invoice:view} — an IAM {@code PermissionCode}. */
    public static String permissionCode(String documentType, String action) {
        validate(documentType);
        return documentType + ":" + action;
    }
}
