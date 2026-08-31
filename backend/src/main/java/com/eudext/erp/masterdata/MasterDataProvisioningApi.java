package com.eudext.erp.masterdata;

import java.time.LocalDate;
import java.util.UUID;

/**
 * ADM-2 / ADM-3: the surface Epic 0.11's tenant onboarding orchestrator
 * uses to create a tenant's first {@code Company} (MDM-1) and seed it with
 * a starter Chart of Accounts and fiscal year (MDM-3, MDM-9) so it is
 * transaction-ready. Read/administer surfaces for master data beyond
 * onboarding are Epic 0.6's own scope.
 */
public interface MasterDataProvisioningApi {

    CompanyView createCompany(UUID tenantId, NewCompany details);

    CompanyView getCompany(UUID companyId);

    /** No-op if the company already has accounts (idempotent re-run safety). */
    void seedDefaultChartOfAccounts(UUID tenantId, UUID companyId, boolean includeSriLankaStatutoryAccounts);

    /** No-op if the company already has a fiscal year; returns the (possibly pre-existing) fiscal year id. */
    UUID seedDefaultFiscalYear(UUID tenantId, UUID companyId, LocalDate fiscalYearStart);

    void disableCompany(UUID companyId);

    record NewCompany(
            String legalName, String registrationNo, String vatNo, String address, String baseCurrency, int fiscalYearStartMonth) {}

    record CompanyView(UUID id, String legalName, String baseCurrency, boolean disabled) {}
}
