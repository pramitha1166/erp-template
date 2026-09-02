package com.eudext.erp.masterdata.internal.uom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** MDM-7: UOM conversion factor invariants. */
class UomConversionTest {

    private final UUID tenantId = UUID.randomUUID();

    @Test
    void createsAValidConversion() {
        UUID box = UUID.randomUUID();
        UUID nos = UUID.randomUUID();

        UomConversion conversion = UomConversion.create(tenantId, box, nos, new BigDecimal("12"));

        assertThat(conversion.getConversionFactor()).isEqualByComparingTo(new BigDecimal("12"));
    }

    @Test
    void rejectsConvertingAUomToItself() {
        UUID uom = UUID.randomUUID();
        assertThatThrownBy(() -> UomConversion.create(tenantId, uom, uom, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANonPositiveFactor() {
        assertThatThrownBy(() -> UomConversion.create(tenantId, UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateFactorRejectsANonPositiveValue() {
        UomConversion conversion = UomConversion.create(tenantId, UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN);
        assertThatThrownBy(() -> conversion.updateFactor(new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
    }
}
