package com.eudext.erp.masterdata.internal.uom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** MDM-7: how many {@code toUom} one {@code fromUom} converts to (e.g. 1 BOX = 12 NOS -> factor 12). */
@Entity
@Table(name = "uom_conversions")
@EntityListeners(AuditingEntityListener.class)
public class UomConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "from_uom_id", nullable = false, updatable = false)
    private UUID fromUomId;

    @Column(name = "to_uom_id", nullable = false, updatable = false)
    private UUID toUomId;

    @Column(name = "conversion_factor", nullable = false)
    private BigDecimal conversionFactor;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected UomConversion() {}

    public static UomConversion create(UUID tenantId, UUID fromUomId, UUID toUomId, BigDecimal conversionFactor) {
        if (fromUomId.equals(toUomId)) {
            throw new IllegalArgumentException("fromUom and toUom must be different units");
        }
        if (conversionFactor.signum() <= 0) {
            throw new IllegalArgumentException("conversionFactor must be positive");
        }
        UomConversion conversion = new UomConversion();
        conversion.tenantId = tenantId;
        conversion.fromUomId = fromUomId;
        conversion.toUomId = toUomId;
        conversion.conversionFactor = conversionFactor;
        return conversion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFromUomId() {
        return fromUomId;
    }

    public UUID getToUomId() {
        return toUomId;
    }

    public BigDecimal getConversionFactor() {
        return conversionFactor;
    }

    public void updateFactor(BigDecimal conversionFactor) {
        if (conversionFactor.signum() <= 0) {
            throw new IllegalArgumentException("conversionFactor must be positive");
        }
        this.conversionFactor = conversionFactor;
    }
}
