package com.eudext.erp.masterdata.internal.item;

/** MDM-6: how an item's stock is costed. Inventory's own costing engine (Epic 0.6's sibling epics) reads this. */
public enum ValuationMethod {
    FIFO,
    WEIGHTED_AVERAGE,
    STANDARD_COST
}
