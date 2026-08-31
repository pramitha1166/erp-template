package com.eudext.erp.masterdata.internal.coa;

import java.util.List;

/**
 * ADM-3: the starter Chart of Accounts a newly onboarded company gets so it
 * is transaction-ready immediately. A small, flat starter set — real
 * hierarchical CoA management (custom accounts, multi-currency, etc.) is
 * Epic 0.6's own scope. The {@code lkStatutory} entries only get seeded
 * when the tenant is entitled to {@code MOD-LK} (BRD-16) — the caller
 * decides that, this template just marks which rows are which so it
 * doesn't have to duplicate the list.
 */
final class ChartOfAccountsSeedTemplate {

    private ChartOfAccountsSeedTemplate() {}

    record Entry(String code, String name, AccountType type, String parentCode, boolean group, boolean lkStatutory) {}

    static List<Entry> standard() {
        return List.of(
                new Entry("1000", "Assets", AccountType.ASSET, null, true, false),
                new Entry("1100", "Current Assets", AccountType.ASSET, "1000", true, false),
                new Entry("1110", "Cash and Bank", AccountType.ASSET, "1100", false, false),
                new Entry("1120", "Accounts Receivable", AccountType.ASSET, "1100", false, false),
                new Entry("1130", "Inventory", AccountType.ASSET, "1100", false, false),
                new Entry("1200", "Fixed Assets", AccountType.ASSET, "1000", true, false),
                new Entry("1210", "Property, Plant and Equipment", AccountType.ASSET, "1200", false, false),
                new Entry("2000", "Liabilities", AccountType.LIABILITY, null, true, false),
                new Entry("2100", "Current Liabilities", AccountType.LIABILITY, "2000", true, false),
                new Entry("2110", "Accounts Payable", AccountType.LIABILITY, "2100", false, false),
                new Entry("2120", "VAT Payable", AccountType.LIABILITY, "2100", false, true),
                new Entry("2130", "EPF Payable", AccountType.LIABILITY, "2100", false, true),
                new Entry("2140", "ETF Payable", AccountType.LIABILITY, "2100", false, true),
                new Entry("2150", "APIT Payable", AccountType.LIABILITY, "2100", false, true),
                new Entry("2200", "Long-term Liabilities", AccountType.LIABILITY, "2000", true, false),
                new Entry("2210", "Loans Payable", AccountType.LIABILITY, "2200", false, false),
                new Entry("3000", "Equity", AccountType.EQUITY, null, true, false),
                new Entry("3100", "Share Capital", AccountType.EQUITY, "3000", false, false),
                new Entry("3200", "Retained Earnings", AccountType.EQUITY, "3000", false, false),
                new Entry("4000", "Income", AccountType.INCOME, null, true, false),
                new Entry("4100", "Sales Revenue", AccountType.INCOME, "4000", false, false),
                new Entry("4200", "Other Income", AccountType.INCOME, "4000", false, false),
                new Entry("5000", "Expenses", AccountType.EXPENSE, null, true, false),
                new Entry("5100", "Cost of Goods Sold", AccountType.EXPENSE, "5000", false, false),
                new Entry("5200", "Operating Expenses", AccountType.EXPENSE, "5000", true, false),
                new Entry("5210", "Salaries and Wages", AccountType.EXPENSE, "5200", false, false),
                new Entry("5220", "Rent Expense", AccountType.EXPENSE, "5200", false, false),
                new Entry("5230", "Utilities Expense", AccountType.EXPENSE, "5200", false, false),
                new Entry("5300", "Statutory Expenses", AccountType.EXPENSE, "5000", true, true),
                new Entry("5310", "EPF Contribution", AccountType.EXPENSE, "5300", false, true),
                new Entry("5320", "ETF Contribution", AccountType.EXPENSE, "5300", false, true));
    }
}
