package com.eudext.erp.masterdata.internal.company;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** MDM-1 / MDM-2: company CRUD — a tenant may hold multiple companies (MDM-2), each an isolated set of ledgers. */
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional
    public Company create(
            UUID tenantId,
            String legalName,
            String registrationNo,
            String vatNo,
            String address,
            String baseCurrency,
            int fiscalYearStartMonth) {
        return companyRepository.save(
                Company.create(tenantId, legalName, registrationNo, vatNo, address, baseCurrency, fiscalYearStartMonth));
    }

    @Transactional(readOnly = true)
    public Company get(UUID companyId) {
        return companyRepository.findById(companyId).orElseThrow(() -> new NoSuchElementException("No such company"));
    }

    /** MDM-2: every company belonging to the current tenant. */
    @Transactional(readOnly = true)
    public List<Company> listForTenant(UUID tenantId) {
        return companyRepository.findByTenantId(tenantId);
    }

    @Transactional
    public Company update(UUID companyId, String legalName, String address, String logoUrl) {
        Company company = get(companyId);
        company.update(legalName, address, logoUrl);
        return companyRepository.save(company);
    }

    @Transactional
    public void disable(UUID companyId) {
        Company company = get(companyId);
        company.disable();
        companyRepository.save(company);
    }

    @Transactional
    public void enable(UUID companyId) {
        Company company = get(companyId);
        company.enable();
        companyRepository.save(company);
    }
}
