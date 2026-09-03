package com.eudext.erp.documents.internal.pdf;

/** DOC-3: wraps a lower-level rendering failure (malformed template XHTML, layout error) behind an unchecked type. */
public class PdfRenderingException extends RuntimeException {

    PdfRenderingException(String message, Throwable cause) {
        super(message, cause);
    }
}
