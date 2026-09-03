package com.eudext.erp.documents.internal.attachment;

/** DOC-4: an upload's content was flagged by the virus scanner. The file is never stored or persisted. */
public class InfectedFileException extends RuntimeException {

    public InfectedFileException(String fileName, String detail) {
        super("Upload rejected, virus scan flagged '" + fileName + "': " + detail);
    }
}
