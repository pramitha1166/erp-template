package com.eudext.erp.documents.internal.dummy;

import com.eudext.erp.config.document.DocumentImmutabilityGuard;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates {@link DummyDocument} through the ARCH-4 lifecycle,
 * demonstrating how a real module's service layer is expected to use the
 * {@code Document} framework: {@link com.eudext.erp.config.document.Document#submit()}
 * / {@code cancel()} / {@code markAmended()} for state transitions, and
 * {@link DocumentImmutabilityGuard} to guard direct field edits.
 */
@Service
public class DummyDocumentService {

    private final DummyDocumentRepository repository;

    public DummyDocumentService(DummyDocumentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DummyDocument create(UUID tenantId, UUID companyId, UUID branchId, String note) {
        return repository.save(DummyDocument.draft(tenantId, companyId, branchId, note));
    }

    @Transactional
    public DummyDocument submit(UUID id, String docNumber) {
        DummyDocument document = get(id);
        document.setDocNumber(docNumber);
        document.submit();
        return repository.save(document);
    }

    @Transactional
    public DummyDocument cancel(UUID id) {
        DummyDocument document = get(id);
        document.cancel();
        return repository.save(document);
    }

    @Transactional
    public DummyDocument updateNote(UUID id, String newNote) {
        DummyDocument document = get(id);
        Map<String, Object> before = DocumentImmutabilityGuard.snapshot(document);
        document.setNote(newNote);
        DocumentImmutabilityGuard.assertNoDisallowedChanges(document, before);
        return repository.save(document);
    }

    /**
     * Amends a submitted document: the original is marked {@code AMENDED}
     * and a new, linked {@code DRAFT} document is created carrying its
     * note forward. The caller submits the replacement separately.
     */
    @Transactional
    public DummyDocument amend(UUID id, String newNote) {
        DummyDocument original = get(id);
        DummyDocument replacement =
                DummyDocument.draft(original.getTenantId(), original.getCompanyId(), original.getBranchId(), newNote);
        replacement.setAmendedFromId(original.getId());
        original.markAmended();
        repository.save(original);
        return repository.save(replacement);
    }

    private DummyDocument get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("No dummy document " + id));
    }
}
