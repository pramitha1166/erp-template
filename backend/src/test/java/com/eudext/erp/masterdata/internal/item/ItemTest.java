package com.eudext.erp.masterdata.internal.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** MDM-6 / MDM-7: item master invariants. */
class ItemTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID itemGroupId = UUID.randomUUID();
    private final UUID stockUomId = UUID.randomUUID();

    @Test
    void createDefaultsToNoPurchaseUomOverrideAndZeroReorderLevel() {
        Item item = Item.create(tenantId, companyId, "ITEM-001", "Widget", itemGroupId, stockUomId, ValuationMethod.FIFO);

        assertThat(item.getPurchaseUomId()).isNull();
        assertThat(item.getReorderLevel()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.isDisabled()).isFalse();
    }

    @Test
    void updateDetailsAllowsADistinctPurchaseUom() {
        Item item = Item.create(tenantId, companyId, "ITEM-002", "Bolt", itemGroupId, stockUomId, ValuationMethod.WEIGHTED_AVERAGE);
        UUID purchaseUomId = UUID.randomUUID();

        item.updateDetails(
                "Bolt (M8)", itemGroupId, purchaseUomId, ValuationMethod.WEIGHTED_AVERAGE, new BigDecimal("100"), true, false, "VAT-STD",
                "7318.15");

        assertThat(item.getPurchaseUomId()).isEqualTo(purchaseUomId);
        assertThat(item.getReorderLevel()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(item.isBatchTracked()).isTrue();
        assertThat(item.getHsCode()).isEqualTo("7318.15");
    }

    @Test
    void rejectsANegativeReorderLevel() {
        Item item = Item.create(tenantId, companyId, "ITEM-003", "Gadget", itemGroupId, stockUomId, ValuationMethod.STANDARD_COST);

        assertThatThrownBy(() -> item.updateDetails(
                        "Gadget", itemGroupId, null, ValuationMethod.STANDARD_COST, new BigDecimal("-1"), false, false, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
