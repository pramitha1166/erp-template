package com.eudext.erp.masterdata.internal.currency;

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

/** MDM-8: an ISO 4217 currency a tenant has enabled for use (e.g. as a business partner's or item price list's currency). */
@Entity
@Table(name = "currencies")
@EntityListeners(AuditingEntityListener.class)
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "code", nullable = false, updatable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "decimal_places", nullable = false)
    private int decimalPlaces;

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

    protected Currency() {}

    public static Currency create(UUID tenantId, String code, String name, String symbol, int decimalPlaces) {
        if (decimalPlaces < 0 || decimalPlaces > 6) {
            throw new IllegalArgumentException("decimalPlaces must be 0-6");
        }
        Currency currency = new Currency();
        currency.tenantId = tenantId;
        currency.code = code;
        currency.name = name;
        currency.symbol = symbol;
        currency.decimalPlaces = decimalPlaces;
        return currency;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public boolean isDisabled() {
        return disabled;
    }

    /** MDM-10: soft-delete only — a Currency is never hard-deleted once it may be referenced. */
    public void disable() {
        this.disabled = true;
    }

    public void enable() {
        this.disabled = false;
    }
}
