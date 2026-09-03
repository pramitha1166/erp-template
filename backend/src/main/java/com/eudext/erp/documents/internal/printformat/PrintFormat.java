package com.eudext.erp.documents.internal.printformat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * DOC-2: a configurable print-format template for a document type — Thymeleaf XHTML (see {@code
 * com.eudext.erp.documents.internal.pdf.PdfRenderer}) merged with document data and rendered server-side to a
 * deterministic PDF (DOC-3). At most one enabled format per company + document type may be the default (enforced by
 * the {@code uq_print_formats_one_default} partial unique index).
 */
@Entity
@Table(name = "print_formats")
@EntityListeners(AuditingEntityListener.class)
public class PrintFormat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "document_type", nullable = false, updatable = false)
    private String documentType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "template_content", nullable = false)
    private String templateContent;

    @Column(name = "disabled", nullable = false)
    private boolean disabled;

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

    protected PrintFormat() {}

    public static PrintFormat create(
            UUID tenantId, UUID companyId, String documentType, String name, String templateContent) {
        PrintFormat printFormat = new PrintFormat();
        printFormat.tenantId = tenantId;
        printFormat.companyId = companyId;
        printFormat.documentType = documentType;
        printFormat.name = name;
        printFormat.templateContent = templateContent;
        return printFormat;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getName() {
        return name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public long getVersion() {
        return version;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void updateTemplate(String templateContent) {
        this.templateContent = templateContent;
    }

    void markDefault() {
        this.isDefault = true;
    }

    void unmarkDefault() {
        this.isDefault = false;
    }

    public void disable() {
        this.disabled = true;
        this.isDefault = false;
    }

    public void enable() {
        this.disabled = false;
    }
}
