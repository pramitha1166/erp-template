package com.eudext.erp.masterdata.internal.company;

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
 * MDM-1: a tenant's company. First created by Epic 0.11's onboarding flow
 * (ADM-2) — {@code disabled} rather than a hard delete once referenced
 * elsewhere, per MDM-10.
 */
@Entity
@Table(name = "companies")
@EntityListeners(AuditingEntityListener.class)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "registration_no")
    private String registrationNo;

    @Column(name = "vat_no")
    private String vatNo;

    @Column(name = "address")
    private String address;

    @Column(name = "base_currency", nullable = false)
    private String baseCurrency;

    /** 1-12; the calendar month a fiscal year starts in (MDM-9 default fiscal year uses this). */
    @Column(name = "fiscal_year_start_month", nullable = false)
    private int fiscalYearStartMonth;

    @Column(name = "logo_url")
    private String logoUrl;

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

    protected Company() {}

    public static Company create(
            UUID tenantId,
            String legalName,
            String registrationNo,
            String vatNo,
            String address,
            String baseCurrency,
            int fiscalYearStartMonth) {
        if (fiscalYearStartMonth < 1 || fiscalYearStartMonth > 12) {
            throw new IllegalArgumentException("fiscalYearStartMonth must be 1-12");
        }
        Company company = new Company();
        company.tenantId = tenantId;
        company.legalName = legalName;
        company.registrationNo = registrationNo;
        company.vatNo = vatNo;
        company.address = address;
        company.baseCurrency = baseCurrency;
        company.fiscalYearStartMonth = fiscalYearStartMonth;
        return company;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public String getVatNo() {
        return vatNo;
    }

    public String getAddress() {
        return address;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public int getFiscalYearStartMonth() {
        return fiscalYearStartMonth;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public boolean isDisabled() {
        return disabled;
    }

    /** MDM-1: the fields an already-onboarded company may amend. Registration/VAT no. and base currency are fixed. */
    public void update(String legalName, String address, String logoUrl) {
        this.legalName = legalName;
        this.address = address;
        this.logoUrl = logoUrl;
    }

    /** MDM-10: soft-delete only — a Company is never hard-deleted once it may be referenced. */
    public void disable() {
        this.disabled = true;
    }

    public void enable() {
        this.disabled = false;
    }
}
