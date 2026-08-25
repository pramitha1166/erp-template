package com.eudext.erp.documents.internal.dummy;

import com.eudext.erp.config.document.Amendable;
import com.eudext.erp.config.document.Document;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Minimal, non-business {@link Document} subclass used only to prove the
 * ARCH-2..ARCH-6 framework works end to end (Phase 0 gate criterion). Real
 * document types (JournalEntry, SalesInvoice, ...) land with their owning
 * modules starting Phase 1.
 */
@Entity
@Table(name = "dummy_documents")
public class DummyDocument extends Document {

    @Column(name = "amended_from_id")
    private UUID amendedFromId;

    /** The one field ARCH-4 permits to change after submission, for test purposes. */
    @Amendable
    @Column(name = "note")
    private String note;

    protected DummyDocument() {}

    public static DummyDocument draft(UUID tenantId, UUID companyId, UUID branchId, String note) {
        DummyDocument document = new DummyDocument();
        document.setTenantId(tenantId);
        document.setCompanyId(companyId);
        document.setBranchId(branchId);
        document.setPostingDate(java.time.LocalDate.now());
        document.note = note;
        return document;
    }

    public UUID getAmendedFromId() {
        return amendedFromId;
    }

    public void setAmendedFromId(UUID amendedFromId) {
        this.amendedFromId = amendedFromId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
