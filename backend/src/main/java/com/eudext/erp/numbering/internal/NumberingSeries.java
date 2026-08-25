package com.eudext.erp.numbering.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * NUM-1: a per-document-type naming series configuration (prefix +
 * zero-padded counter). {@code nextCounter} is tracked here for a future
 * allocation service (NUM-2/NUM-4's gapless, concurrency-safe allocation
 * is Epic 0.5's own scope, not this epic's) — Epic 0.11 only needs to seed
 * a tenant's default series set at onboarding time (ADM-3).
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

    public UUID getId() {
        return id;
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
}
