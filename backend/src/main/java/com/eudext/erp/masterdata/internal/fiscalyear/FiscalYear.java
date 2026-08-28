package com.eudext.erp.masterdata.internal.fiscalyear;

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
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** MDM-9: a company's fiscal year, open until explicitly closed. */
@Entity
@Table(name = "fiscal_years")
@EntityListeners(AuditingEntityListener.class)
public class FiscalYear {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "start_date", nullable = false, updatable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false, updatable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FiscalYearStatus status = FiscalYearStatus.OPEN;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected FiscalYear() {}

    public static FiscalYear of(UUID tenantId, UUID companyId, String name, LocalDate startDate, LocalDate endDate) {
        FiscalYear fiscalYear = new FiscalYear();
        fiscalYear.tenantId = tenantId;
        fiscalYear.companyId = companyId;
        fiscalYear.name = name;
        fiscalYear.startDate = startDate;
        fiscalYear.endDate = endDate;
        return fiscalYear;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public FiscalYearStatus getStatus() {
        return status;
    }

    public void close() {
        this.status = FiscalYearStatus.CLOSED;
    }
}
