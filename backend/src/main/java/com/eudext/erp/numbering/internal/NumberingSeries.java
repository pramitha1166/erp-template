package com.eudext.erp.numbering.internal;

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

/**
 * NUM-1: a per-document-type naming series configuration — a prefix
 * template (which may contain the {@code {YYYY}}/{@code {YY}}/{@code {MM}}/
 * {@code {FY}} date-part placeholders resolved by {@link
 * SeriesNumberFormatter}) plus a zero-padded counter. {@code nextCounter}
 * and {@code currentPeriodKey} are mutated only by {@link
 * NumberAllocationService}'s pessimistic-locked allocation (NUM-2 gapless,
 * NUM-4 concurrency-safe); {@code resetPolicy}/{@code fiscalYearStartMonth}
 * make the NUM-3 fiscal-year reset behaviour configurable per series.
 */
@Entity
@Table(name = "numbering_series")
@EntityListeners(AuditingEntityListener.class)
public class NumberingSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "doc_type", nullable = false, updatable = false)
    private String docType;

    @Column(name = "prefix", nullable = false)
    private String prefix;

    @Column(name = "counter_width", nullable = false)
    private int counterWidth;

    @Column(name = "next_counter", nullable = false)
    private long nextCounter = 1;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "reset_policy", nullable = false)
    private NumberingResetPolicy resetPolicy = NumberingResetPolicy.NEVER;

    @Column(name = "fiscal_year_start_month", nullable = false)
    private int fiscalYearStartMonth = 1;

    @Column(name = "current_period_key")
    private String currentPeriodKey;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected NumberingSeries() {}

    public static NumberingSeries create(UUID tenantId, UUID companyId, String docType, String prefix, int counterWidth) {
        NumberingSeries series = new NumberingSeries();
        series.tenantId = tenantId;
        series.companyId = companyId;
        series.docType = docType;
        series.prefix = prefix;
        series.counterWidth = counterWidth;
        return series;
    }

    /** NUM-1/NUM-3: updates the naming template and reset behaviour; never touches the live counter or period key. */
    public void configure(String prefix, int counterWidth, NumberingResetPolicy resetPolicy, int fiscalYearStartMonth) {
        if (resetPolicy == NumberingResetPolicy.ANNUAL && (fiscalYearStartMonth < 1 || fiscalYearStartMonth > 12)) {
            throw new IllegalArgumentException("fiscalYearStartMonth must be 1..12, got: " + fiscalYearStartMonth);
        }
        this.prefix = prefix;
        this.counterWidth = counterWidth;
        this.resetPolicy = resetPolicy;
        this.fiscalYearStartMonth = fiscalYearStartMonth;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    /**
     * NUM-2/NUM-4: allocates the next counter value, rolling over to 1 first if {@code onDate} has moved into a new
     * reset period. Callers must hold this row's pessimistic write lock (see {@code
     * NumberingSeriesRepository#findForUpdate}) — this method only mutates in-memory state.
     */
    long allocate(LocalDate onDate) {
        String periodKey = SeriesNumberFormatter.periodKeyFor(resetPolicy, onDate, fiscalYearStartMonth);
        if (periodKey != null && !periodKey.equals(currentPeriodKey)) {
            this.nextCounter = 1;
            this.currentPeriodKey = periodKey;
        }
        long allocated = nextCounter;
        nextCounter++;
        return allocated;
    }

    String resolvedPrefix(LocalDate onDate) {
        return SeriesNumberFormatter.resolvePrefix(prefix, onDate, fiscalYearStartMonth);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getDocType() {
        return docType;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getCounterWidth() {
        return counterWidth;
    }

    public boolean isActive() {
        return active;
    }

    public NumberingResetPolicy getResetPolicy() {
        return resetPolicy;
    }

    public int getFiscalYearStartMonth() {
        return fiscalYearStartMonth;
    }

    public long getNextCounter() {
        return nextCounter;
    }

    public long getVersion() {
        return version;
    }
}
