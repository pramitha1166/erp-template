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

/** MDM-9: one open/closed accounting period within a {@link FiscalYear} — monthly by default. */
@Entity
@Table(name = "accounting_periods")
@EntityListeners(AuditingEntityListener.class)
public class AccountingPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "fiscal_year_id", nullable = false, updatable = false)
    private UUID fiscalYearId;

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

    protected AccountingPeriod() {}

    public static AccountingPeriod of(
            UUID tenantId, UUID companyId, UUID fiscalYearId, String name, LocalDate startDate, LocalDate endDate) {
        AccountingPeriod period = new AccountingPeriod();
        period.tenantId = tenantId;
        period.companyId = companyId;
        period.fiscalYearId = fiscalYearId;
        period.name = name;
        period.startDate = startDate;
        period.endDate = endDate;
        return period;
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

    public UUID getFiscalYearId() {
        return fiscalYearId;
    }

    public FiscalYearStatus getStatus() {
        return status;
    }

    public void close() {
        this.status = FiscalYearStatus.CLOSED;
    }

    public void reopen() {
        this.status = FiscalYearStatus.OPEN;
    }
}
