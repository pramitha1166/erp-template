package com.eudext.erp.documents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eudext.erp.config.document.DocStatus;
import com.eudext.erp.config.document.DocumentImmutabilityGuard;
import com.eudext.erp.config.document.DocumentImmutableException;
import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.documents.internal.dummy.DummyDocument;
import com.eudext.erp.documents.internal.dummy.DummyDocumentRepository;
import com.eudext.erp.documents.internal.dummy.DummyDocumentService;
import com.eudext.erp.testsupport.AbstractIntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Phase 0 gate criterion: "a dummy document completes full lifecycle."
 * Drives {@link DummyDocument} through create -> submit -> (blocked)
 * mutation -> allowed (amendable) mutation -> cancel, and separately
 * through submit -> amend, against a real Postgres via Testcontainers.
 */
class DummyDocumentLifecycleIT extends AbstractIntegrationTest {

    @Autowired
    private DummyDocumentService service;

    @Autowired
    private DummyDocumentRepository repository;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(tenantId);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void draftSubmitBlockedMutationAllowedMutationThenCancel() {
        DummyDocument draft = service.create(tenantId, companyId, branchId, "initial note");
        assertThat(draft.getDocStatus()).isEqualTo(DocStatus.DRAFT);
        assertThat(draft.getVersion()).isEqualTo(0);

        DummyDocument submitted = service.submit(draft.getId(), "DUMMY-0001");
        assertThat(submitted.getDocStatus()).isEqualTo(DocStatus.SUBMITTED);
        assertThat(submitted.getDocNumber()).isEqualTo("DUMMY-0001");
        assertThat(submitted.getVersion()).isGreaterThan(draft.getVersion());

        DummyDocument persisted = repository.findById(submitted.getId()).orElseThrow();
        Map<String, Object> before = DocumentImmutabilityGuard.snapshot(persisted);
        persisted.setPostingDate(persisted.getPostingDate().plusDays(1));
        assertThatThrownBy(() -> DocumentImmutabilityGuard.assertNoDisallowedChanges(persisted, before))
                .isInstanceOf(DocumentImmutableException.class);

        DummyDocument noted = service.updateNote(submitted.getId(), "note changed after submission");
        assertThat(noted.getNote()).isEqualTo("note changed after submission");
        assertThat(noted.getDocStatus()).isEqualTo(DocStatus.SUBMITTED);

        DummyDocument cancelled = service.cancel(noted.getId());
        assertThat(cancelled.getDocStatus()).isEqualTo(DocStatus.CANCELLED);
    }

    @Test
    void submittedDocumentAmendsToANewLinkedDraft() {
        DummyDocument draft = service.create(tenantId, companyId, branchId, "original note");
        DummyDocument submitted = service.submit(draft.getId(), "DUMMY-0002");

        DummyDocument replacement = service.amend(submitted.getId(), "amended note");

        DummyDocument amendedOriginal = repository.findById(submitted.getId()).orElseThrow();
        assertThat(amendedOriginal.getDocStatus()).isEqualTo(DocStatus.AMENDED);

        assertThat(replacement.getDocStatus()).isEqualTo(DocStatus.DRAFT);
        assertThat(replacement.getAmendedFromId()).isEqualTo(amendedOriginal.getId());
        assertThat(replacement.getNote()).isEqualTo("amended note");
        assertThat(replacement.getTenantId()).isEqualTo(tenantId);
        assertThat(replacement.getBranchId()).isEqualTo(branchId);
    }
}
