package com.eudext.erp.masterdata.internal.provisioning;

import com.eudext.erp.masterdata.MasterDataProvisioningApi;
import com.eudext.erp.masterdata.internal.coa.ChartOfAccountsSeedService;
import com.eudext.erp.masterdata.internal.company.Company;
import com.eudext.erp.masterdata.internal.company.CompanyService;
import com.eudext.erp.masterdata.internal.fiscalyear.FiscalYearSeedService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class MasterDataProvisioningServiceImpl implements MasterDataProvisioningApi {

    private final CompanyService companyService;
    private final ChartOfAccountsSeedService chartOfAccountsSeedService;
    private final FiscalYearSeedService fiscalYearSeedService;

    MasterDataProvisioningServiceImpl(
            CompanyService companyService,
            ChartOfAccountsSeedService chartOfAccountsSeedService,
            FiscalYearSeedService fiscalYearSeedService) {
        this.companyService = companyService;
        this.chartOfAccountsSeedService = chartOfAccountsSeedService;
        this.fiscalYearSeedService = fiscalYearSeedService;
    }

    @Override
    public CompanyView createCompany(UUID tenantId, NewCompany details) {
        Company company = companyService.create(
                tenantId,
                details.legalName(),
                details.registrationNo(),
                details.vatNo(),
                details.address(),
                details.baseCurrency(),
                details.fiscalYearStartMonth());
        return toView(company);
    }

    @Override
    public CompanyView getCompany(UUID companyId) {
        return toView(companyService.get(companyId));
    }

    @Override
    public void seedDefaultChartOfAccounts(UUID tenantId, UUID companyId, boolean includeSriLankaStatutoryAccounts) {
        chartOfAccountsSeedService.seedDefault(tenantId, companyId, includeSriLankaStatutoryAccounts);
    }

    @Override
    public UUID seedDefaultFiscalYear(UUID tenantId, UUID companyId, LocalDate fiscalYearStart) {
        return fiscalYearSeedService.seedDefault(tenantId, companyId, fiscalYearStart);
    }

    @Override
    public void disableCompany(UUID companyId) {
        companyService.disable(companyId);
    }

    private static CompanyView toView(Company company) {
        return new CompanyView(company.getId(), company.getLegalName(), company.getBaseCurrency(), company.isDisabled());
    }
}
