package com.eudext.erp.config.document;

/** Minimal, non-JPA {@link Document} subclass for framework unit tests. */
class TestDocument extends Document {

    @Amendable
    private String amendableNote;

    private String frozenField;

    TestDocument() {}

    String getAmendableNote() {
        return amendableNote;
    }

    void setAmendableNote(String amendableNote) {
        this.amendableNote = amendableNote;
    }

    String getFrozenField() {
        return frozenField;
    }

    void setFrozenField(String frozenField) {
        this.frozenField = frozenField;
    }
}
