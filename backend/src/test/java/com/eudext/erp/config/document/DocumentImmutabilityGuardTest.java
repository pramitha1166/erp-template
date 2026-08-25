package com.eudext.erp.config.document;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * ARCH-4: a document past {@code DRAFT} is immutable except for fields
 * marked {@link Amendable}.
 */
class DocumentImmutabilityGuardTest {

    @Test
    void draftDocumentAllowsAnyFieldChange() {
        TestDocument document = new TestDocument();
        Map<String, Object> before = DocumentImmutabilityGuard.snapshot(document);

        document.setFrozenField("anything");

        assertThatCode(() -> DocumentImmutabilityGuard.assertNoDisallowedChanges(document, before))
                .doesNotThrowAnyException();
    }

    @Test
    void submittedDocumentAllowsAmendableFieldChange() {
        TestDocument document = submittedDocument();
        Map<String, Object> before = DocumentImmutabilityGuard.snapshot(document);

        document.setAmendableNote("updated");

        assertThatCode(() -> DocumentImmutabilityGuard.assertNoDisallowedChanges(document, before))
                .doesNotThrowAnyException();
    }

    @Test
    void submittedDocumentRejectsNonAmendableFieldChange() {
        TestDocument document = submittedDocument();
        Map<String, Object> before = DocumentImmutabilityGuard.snapshot(document);

        document.setFrozenField("changed");

        assertThatThrownBy(() -> DocumentImmutabilityGuard.assertNoDisallowedChanges(document, before))
                .isInstanceOf(DocumentImmutableException.class);
    }

    @Test
    void submittedDocumentAllowsFrameworkManagedTransitions() {
        TestDocument document = submittedDocument();
        Map<String, Object> before = DocumentImmutabilityGuard.snapshot(document);

        document.cancel();

        assertThatCode(() -> DocumentImmutabilityGuard.assertNoDisallowedChanges(document, before))
                .doesNotThrowAnyException();
    }

    private static TestDocument submittedDocument() {
        TestDocument document = new TestDocument();
        document.setDocNumber("DUMMY-0001");
        document.setFrozenField("original");
        document.submit();
        return document;
    }
}
