package com.eudext.erp.masterdata.internal.company;

import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** MDM-1: company CRUD. */
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

    @Transactional
    public void disable(UUID companyId) {
        Company company = get(companyId);
        company.disable();
        companyRepository.save(company);
    }
}
