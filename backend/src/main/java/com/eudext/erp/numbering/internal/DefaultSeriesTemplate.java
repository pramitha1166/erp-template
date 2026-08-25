package com.eudext.erp.numbering.internal;

import java.util.List;

/** ADM-3: the naming series a newly onboarded company gets seeded with by default. */
final class DefaultSeriesTemplate {

    private DefaultSeriesTemplate() {}

    record Entry(String docType, String prefix, int counterWidth) {}

    static List<Entry> standard() {
        return List.of(
                new Entry("SALES_INVOICE", "SINV-", 5),
                new Entry("PURCHASE_INVOICE", "PINV-", 5),
                new Entry("PAYMENT_VOUCHER", "PAY-", 5),
                new Entry("JOURNAL_VOUCHER", "JV-", 5),
                new Entry("GOODS_RECEIPT_NOTE", "GRN-", 5));
    }
}
