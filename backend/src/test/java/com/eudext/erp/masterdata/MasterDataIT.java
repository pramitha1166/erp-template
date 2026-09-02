package com.eudext.erp.masterdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.masterdata.internal.coa.AccountService;
import com.eudext.erp.masterdata.internal.coa.AccountType;
import com.eudext.erp.masterdata.internal.company.Company;
import com.eudext.erp.masterdata.internal.company.CompanyService;
import com.eudext.erp.masterdata.internal.costcentre.CostCentreService;
import com.eudext.erp.masterdata.internal.currency.CurrencyService;
import com.eudext.erp.masterdata.internal.currency.ExchangeRateSource;
import com.eudext.erp.masterdata.internal.fiscalyear.AccountingPeriodService;
import com.eudext.erp.masterdata.internal.fiscalyear.FiscalYear;
import com.eudext.erp.masterdata.internal.fiscalyear.FiscalYearSeedService;
import com.eudext.erp.masterdata.internal.fiscalyear.FiscalYearService;
import com.eudext.erp.masterdata.internal.fiscalyear.FiscalYearStatus;
import com.eudext.erp.masterdata.internal.item.Item;
import com.eudext.erp.masterdata.internal.item.ItemGroupService;
import com.eudext.erp.masterdata.internal.item.ItemService;
import com.eudext.erp.masterdata.internal.item.ValuationMethod;
import com.eudext.erp.masterdata.internal.partner.BusinessPartnerService;
import com.eudext.erp.masterdata.internal.partner.BusinessPartnerType;
import com.eudext.erp.masterdata.internal.uom.UnitOfMeasure;
import com.eudext.erp.masterdata.internal.uom.UomService;
import com.eudext.erp.testsupport.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Epic 0.6 (PLAT-MDM) end to end against a real Postgres: company (MDM-1/2), Chart of Accounts (MDM-3), cost centres
 * (MDM-4), business partners (MDM-5), items (MDM-6), UOM conversions (MDM-7), currencies (MDM-8), fiscal years
 * (MDM-9), and the soft-delete-only invariant (MDM-10) — all under RLS.
 */
class MasterDataIT extends AbstractIntegrationTest {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CostCentreService costCentreService;

    @Autowired
    private BusinessPartnerService partnerService;

    @Autowired
    private ItemGroupService itemGroupService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UomService uomService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private FiscalYearSeedService fiscalYearSeedService;

    @Autowired
    private FiscalYearService fiscalYearService;

    @Autowired
    private AccountingPeriodService accountingPeriodService;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(tenantId);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void companyIsCreatedAndRLSScopesItToItsOwnTenant() {
        Company company = companyService.create(tenantId, "Acme (Pvt) Ltd", "REG-1", "VAT-1", "Colombo", "LKR", 1);

        assertThat(companyService.listForTenant(tenantId)).extracting(Company::getId).contains(company.getId());

        TenantContext.set(UUID.randomUUID());
        assertThat(companyService.listForTenant(tenantId)).isEmpty();
    }

    @Test
    void chartOfAccountsEnforcesGroupVsLedgerHierarchy() {
        UUID companyId = companyService.create(tenantId, "CoA Co", null, null, null, "LKR", 1).getId();

        var assets = accountService.create(tenantId, companyId, "1000", "Assets", AccountType.ASSET, null, true);
        var cash = accountService.create(tenantId, companyId, "1100", "Cash", AccountType.ASSET, assets.getId(), false);

        assertThat(cash.getParentId()).isEqualTo(assets.getId());
        assertThatThrownBy(() -> accountService.create(tenantId, companyId, "1101", "Petty Cash", AccountType.ASSET, cash.getId(), false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void costCentreIsSoftDeletedOnlyViaDisable() {
        UUID companyId = companyService.create(tenantId, "CC Co", null, null, null, "LKR", 1).getId();

        var headOffice = costCentreService.create(tenantId, companyId, "HO", "Head Office", null);
        costCentreService.disable(headOffice.getId());

        assertThat(costCentreService.get(headOffice.getId()).isDisabled()).isTrue();
        assertThat(costCentreService.listForCompany(companyId)).extracting("id").contains(headOffice.getId());
    }

    @Test
    void businessPartnerCarriesContactsCreditTermsAndBankDetails() {
        UUID companyId = companyService.create(tenantId, "BP Co", null, null, null, "LKR", 1).getId();

        var customer = partnerService.create(tenantId, companyId, BusinessPartnerType.CUSTOMER, "CUST-001", "Acme Traders");
        partnerService.update(
                customer.getId(), "Acme Traders (Pvt) Ltd", "VAT-999", new BigDecimal("100000"), 45, null, "BOC", "Colombo",
                "0011223344", "BCEYLKLX");
        partnerService.addContact(customer.getId(), "Jane Silva", "Finance Manager", "0771234567", "jane@acme.lk", true);

        assertThat(partnerService.get(customer.getId()).getCreditTermsDays()).isEqualTo(45);
        assertThat(partnerService.listContacts(customer.getId())).hasSize(1);
        assertThat(partnerService.listForCompany(companyId, BusinessPartnerType.CUSTOMER)).extracting("id").contains(customer.getId());
    }

    @Test
    void itemUsesADistinctPurchaseUomFromItsStockUom() {
        UUID companyId = companyService.create(tenantId, "Item Co", null, null, null, "LKR", 1).getId();
        var itemGroup = itemGroupService.create(tenantId, companyId, "RAW", "Raw Materials", null);

        UnitOfMeasure nos = uomService.create(tenantId, "NOS", "Numbers");
        UnitOfMeasure box = uomService.create(tenantId, "BOX", "Box");
        uomService.configureConversion(tenantId, box.getId(), nos.getId(), new BigDecimal("12"));

        Item item = itemService.create(tenantId, companyId, "ITEM-001", "Widget", itemGroup.getId(), nos.getId(), ValuationMethod.FIFO);
        item = itemService.update(
                item.getId(), "Widget", itemGroup.getId(), box.getId(), ValuationMethod.FIFO, new BigDecimal("50"), false, false,
                "VAT-STD", "8481.80");

        assertThat(item.getStockUomId()).isEqualTo(nos.getId());
        assertThat(item.getPurchaseUomId()).isEqualTo(box.getId());
        assertThat(uomService.conversionsFrom(box.getId())).extracting("conversionFactor")
                .containsExactly(new BigDecimal("12.000000"));
    }

    @Test
    void currencyMustBeEnabledBeforeARateCanBeRecordedAgainstIt() {
        assertThatThrownBy(() -> currencyService.recordRate(tenantId, "USD", LocalDate.now(), BigDecimal.TEN, ExchangeRateSource.MANUAL))
                .isInstanceOf(NoSuchElementException.class);

        currencyService.create(tenantId, "USD", "US Dollar", "$", 2);
        currencyService.recordRate(tenantId, "USD", LocalDate.of(2026, 1, 1), new BigDecimal("300"), ExchangeRateSource.MANUAL);
        currencyService.recordRate(tenantId, "USD", LocalDate.of(2026, 2, 1), new BigDecimal("305"), ExchangeRateSource.MANUAL);

        assertThat(currencyService.rateAsOf(tenantId, "USD", LocalDate.of(2026, 1, 15)).getRateToBase())
                .isEqualByComparingTo(new BigDecimal("300"));
        assertThat(currencyService.rateAsOf(tenantId, "USD", LocalDate.of(2026, 3, 1)).getRateToBase())
                .isEqualByComparingTo(new BigDecimal("305"));
    }

    @Test
    void fiscalYearCannotCloseWhileAnAccountingPeriodIsStillOpen() {
        UUID companyId = companyService.create(tenantId, "FY Co", null, null, null, "LKR", 1).getId();
        UUID fiscalYearId = fiscalYearSeedService.seedDefault(tenantId, companyId, LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> fiscalYearService.close(fiscalYearId)).isInstanceOf(IllegalStateException.class);

        for (var period : accountingPeriodService.listForFiscalYear(fiscalYearId)) {
            accountingPeriodService.close(period.getId());
        }
        fiscalYearService.close(fiscalYearId);

        FiscalYear closed = fiscalYearService.get(fiscalYearId);
        assertThat(closed.getStatus()).isEqualTo(FiscalYearStatus.CLOSED);
    }
}
