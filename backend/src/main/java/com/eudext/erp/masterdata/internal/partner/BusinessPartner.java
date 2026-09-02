package com.eudext.erp.masterdata.internal.partner;

import com.eudext.erp.config.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** MDM-5: a customer/supplier master — tax registration, credit terms, a default GL account, and bank details. */
@Entity
@Table(name = "business_partners")
@EntityListeners(AuditingEntityListener.class)
public class BusinessPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", nullable = false, updatable = false)
    private BusinessPartnerType partnerType;

    @Column(name = "code", nullable = false, updatable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "tax_registration_no")
    private String taxRegistrationNo;

    @Column(name = "credit_limit", nullable = false)
    private BigDecimal creditLimit = Money.zero();

    @Column(name = "credit_terms_days", nullable = false)
    private int creditTermsDays;

    @Column(name = "default_account_id")
    private UUID defaultAccountId;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_branch")
    private String bankBranch;

    @Column(name = "bank_account_no")
    private String bankAccountNo;

    @Column(name = "bank_swift_code")
    private String bankSwiftCode;

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

    protected BusinessPartner() {}

    public static BusinessPartner create(UUID tenantId, UUID companyId, BusinessPartnerType partnerType, String code, String name) {
        BusinessPartner partner = new BusinessPartner();
        partner.tenantId = tenantId;
        partner.companyId = companyId;
        partner.partnerType = partnerType;
        partner.code = code;
        partner.name = name;
        return partner;
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

    public BusinessPartnerType getPartnerType() {
        return partnerType;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getTaxRegistrationNo() {
        return taxRegistrationNo;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public int getCreditTermsDays() {
        return creditTermsDays;
    }

    public UUID getDefaultAccountId() {
        return defaultAccountId;
    }

    public String getBankName() {
        return bankName;
    }

    public String getBankBranch() {
        return bankBranch;
    }

    public String getBankAccountNo() {
        return bankAccountNo;
    }

    public String getBankSwiftCode() {
        return bankSwiftCode;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void updateDetails(
            String name,
            String taxRegistrationNo,
            BigDecimal creditLimit,
            int creditTermsDays,
            UUID defaultAccountId,
            String bankName,
            String bankBranch,
            String bankAccountNo,
            String bankSwiftCode) {
        if (creditTermsDays < 0) {
            throw new IllegalArgumentException("creditTermsDays must not be negative");
        }
        this.name = name;
        this.taxRegistrationNo = taxRegistrationNo;
        this.creditLimit = Money.scale(creditLimit);
        this.creditTermsDays = creditTermsDays;
        this.defaultAccountId = defaultAccountId;
        this.bankName = bankName;
        this.bankBranch = bankBranch;
        this.bankAccountNo = bankAccountNo;
        this.bankSwiftCode = bankSwiftCode;
    }

    /** MDM-10: soft-delete only — a BusinessPartner is never hard-deleted once it may be referenced. */
    public void disable() {
        this.disabled = true;
    }

    public void enable() {
        this.disabled = false;
    }
}
