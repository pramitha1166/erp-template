package com.eudext.erp.config.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * ARCH-4: verifies the {@code DRAFT -> SUBMITTED -> CANCELLED} / {@code
 * SUBMITTED -> AMENDED} state machine and rejects every other transition.
 */
class DocumentLifecycleTest {

    @Test
    void startsInDraft() {
        assertThat(new TestDocument().getDocStatus()).isEqualTo(DocStatus.DRAFT);
    }

    @Test
    void cannotSubmitWithoutADocNumber() {
        TestDocument document = new TestDocument();

        assertThatThrownBy(document::submit).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void draftSubmitsToSubmitted() {
        TestDocument document = new TestDocument();
        document.setDocNumber("DUMMY-0001");

        document.submit();

        assertThat(document.getDocStatus()).isEqualTo(DocStatus.SUBMITTED);
    }

    @Test
    void submittedCancels() {
        TestDocument document = submittedDocument();

        document.cancel();

        assertThat(document.getDocStatus()).isEqualTo(DocStatus.CANCELLED);
    }

    @Test
    void submittedAmends() {
        TestDocument document = submittedDocument();

        document.markAmended();

        assertThat(document.getDocStatus()).isEqualTo(DocStatus.AMENDED);
    }

    @Test
    void draftCannotCancel() {
        TestDocument document = new TestDocument();

        assertThatThrownBy(document::cancel).isInstanceOf(IllegalDocumentTransitionException.class);
    }

    @Test
    void draftCannotBeAmended() {
        TestDocument document = new TestDocument();

        assertThatThrownBy(document::markAmended).isInstanceOf(IllegalDocumentTransitionException.class);
    }

    @Test
    void cancelledIsTerminal() {
        TestDocument document = submittedDocument();
        document.cancel();

        assertThatThrownBy(document::submit).isInstanceOf(IllegalDocumentTransitionException.class);
        assertThatThrownBy(document::cancel).isInstanceOf(IllegalDocumentTransitionException.class);
        assertThatThrownBy(document::markAmended).isInstanceOf(IllegalDocumentTransitionException.class);
    }

    @Test
    void amendedIsTerminal() {
        TestDocument document = submittedDocument();
        document.markAmended();

        assertThatThrownBy(document::submit).isInstanceOf(IllegalDocumentTransitionException.class);
        assertThatThrownBy(document::cancel).isInstanceOf(IllegalDocumentTransitionException.class);
        assertThatThrownBy(document::markAmended).isInstanceOf(IllegalDocumentTransitionException.class);
    }

    @Test
    void submittedCannotResubmit() {
        TestDocument document = submittedDocument();

        assertThatThrownBy(document::submit).isInstanceOf(IllegalDocumentTransitionException.class);
    }

    private static TestDocument submittedDocument() {
        TestDocument document = new TestDocument();
        document.setDocNumber("DUMMY-0001");
        document.submit();
        return document;
    }
}
