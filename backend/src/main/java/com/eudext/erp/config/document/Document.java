package com.eudext.erp.config.document;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * ARCH-3: common supertype for every transactional document. Carries the
 * multi-tenancy discriminator (ARCH-2), the ARCH-4 lifecycle state machine,
 * and optimistic locking (ARCH-6).
 *
 * <p>{@code branchId} is included here — not part of the ARCH-3 field list
 * in the SRS, but ORG-2 requires it {@code NOT NULL} on every transactional
 * document from the migration that first creates its table, since it cannot
 * be safely retrofitted later. Epic 0.9 wires it to the Branch master and
 * enforces record-visibility filtering; until then it is an opaque
 * caller-supplied identifier.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Document implements Serializable {

    /** ARCH-4: the only transitions the state machine permits. */
    private static final Map<DocStatus, Set<DocStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(DocStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(DocStatus.DRAFT, EnumSet.of(DocStatus.SUBMITTED));
        ALLOWED_TRANSITIONS.put(DocStatus.SUBMITTED, EnumSet.of(DocStatus.CANCELLED, DocStatus.AMENDED));
        ALLOWED_TRANSITIONS.put(DocStatus.CANCELLED, EnumSet.noneOf(DocStatus.class));
        ALLOWED_TRANSITIONS.put(DocStatus.AMENDED, EnumSet.noneOf(DocStatus.class));
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "doc_number", updatable = false)
    private String docNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_status", nullable = false)
    private DocStatus docStatus = DocStatus.DRAFT;

    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedBy
    @Column(name = "modified_by")
    private String modifiedBy;

    @LastModifiedDate
    @Column(name = "modified_at")
    private Instant modifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Document() {}

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public void setDocNumber(String docNumber) {
        this.docNumber = docNumber;
    }

    public DocStatus getDocStatus() {
        return docStatus;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public long getVersion() {
        return version;
    }

    /**
     * Moves this document from {@code DRAFT} to {@code SUBMITTED}. A
     * {@code docNumber} must already be assigned — numbering itself is
     * Epic 0.5's concern; this only guards that a submitted document is
     * never left without one.
     */
    public final void submit() {
        if (docNumber == null || docNumber.isBlank()) {
            throw new IllegalStateException("Cannot submit a document without a docNumber assigned");
        }
        transitionTo(DocStatus.SUBMITTED);
    }

    /** Moves this document from {@code SUBMITTED} to {@code CANCELLED}. */
    public final void cancel() {
        transitionTo(DocStatus.CANCELLED);
    }

    /**
     * Marks this document {@code AMENDED} — called on the original once a
     * new, linked replacement document has been created. The caller owns
     * creating and populating that replacement; this only closes out the
     * original per the ARCH-4 state machine.
     */
    public final void markAmended() {
        transitionTo(DocStatus.AMENDED);
    }

    private void transitionTo(DocStatus target) {
        Set<DocStatus> allowed = ALLOWED_TRANSITIONS.get(docStatus);
        if (allowed == null || !allowed.contains(target)) {
            throw new IllegalDocumentTransitionException(docStatus, target);
        }
        this.docStatus = target;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Document that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}
