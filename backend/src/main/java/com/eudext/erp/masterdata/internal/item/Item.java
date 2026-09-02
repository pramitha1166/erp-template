package com.eudext.erp.masterdata.internal.item;

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

/**
 * MDM-6: an item master. {@code purchaseUomId} may differ from {@code stockUomId} per MDM-7 (e.g. purchased by the
 * box, stocked by the unit) — {@code null} means purchasing uses the stock UOM directly.
 */
@Entity
@Table(name = "items")
@EntityListeners(AuditingEntityListener.class)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "code", nullable = false, updatable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "item_group_id", nullable = false)
    private UUID itemGroupId;

    @Column(name = "stock_uom_id", nullable = false, updatable = false)
    private UUID stockUomId;

    @Column(name = "purchase_uom_id")
    private UUID purchaseUomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "valuation_method", nullable = false)
    private ValuationMethod valuationMethod;

    @Column(name = "reorder_level", nullable = false)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(name = "batch_tracked", nullable = false)
    private boolean batchTracked;

    @Column(name = "serial_tracked", nullable = false)
    private boolean serialTracked;

    @Column(name = "tax_category_code")
    private String taxCategoryCode;

    @Column(name = "hs_code")
    private String hsCode;

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

    protected Item() {}

    public static Item create(
            UUID tenantId, UUID companyId, String code, String name, UUID itemGroupId, UUID stockUomId, ValuationMethod valuationMethod) {
        Item item = new Item();
        item.tenantId = tenantId;
        item.companyId = companyId;
        item.code = code;
        item.name = name;
        item.itemGroupId = itemGroupId;
        item.stockUomId = stockUomId;
        item.valuationMethod = valuationMethod;
        return item;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public UUID getItemGroupId() {
        return itemGroupId;
    }

    public UUID getStockUomId() {
        return stockUomId;
    }

    public UUID getPurchaseUomId() {
        return purchaseUomId;
    }

    public ValuationMethod getValuationMethod() {
        return valuationMethod;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public boolean isBatchTracked() {
        return batchTracked;
    }

    public boolean isSerialTracked() {
        return serialTracked;
    }

    public String getTaxCategoryCode() {
        return taxCategoryCode;
    }

    public String getHsCode() {
        return hsCode;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void updateDetails(
            String name,
            UUID itemGroupId,
            UUID purchaseUomId,
            ValuationMethod valuationMethod,
            BigDecimal reorderLevel,
            boolean batchTracked,
            boolean serialTracked,
            String taxCategoryCode,
            String hsCode) {
        if (reorderLevel.signum() < 0) {
            throw new IllegalArgumentException("reorderLevel must not be negative");
        }
        this.name = name;
        this.itemGroupId = itemGroupId;
        this.purchaseUomId = purchaseUomId;
        this.valuationMethod = valuationMethod;
        this.reorderLevel = reorderLevel;
        this.batchTracked = batchTracked;
        this.serialTracked = serialTracked;
        this.taxCategoryCode = taxCategoryCode;
        this.hsCode = hsCode;
    }

    /** MDM-10: soft-delete only — an Item is never hard-deleted once it may be referenced. */
    public void disable() {
        this.disabled = true;
    }

    public void enable() {
        this.disabled = false;
    }
}
