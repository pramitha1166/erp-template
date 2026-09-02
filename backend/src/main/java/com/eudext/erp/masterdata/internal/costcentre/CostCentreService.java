package com.eudext.erp.masterdata.internal.costcentre;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** MDM-4: cost centre CRUD, hierarchical via {@code parentId}. */
@Service
public class CostCentreService {

    private final CostCentreRepository costCentreRepository;

    public CostCentreService(CostCentreRepository costCentreRepository) {
        this.costCentreRepository = costCentreRepository;
    }

    @Transactional
    public CostCentre create(UUID tenantId, UUID companyId, String code, String name, UUID parentId) {
        if (parentId != null && !costCentreRepository.existsById(parentId)) {
            throw new NoSuchElementException("No such parent cost centre");
        }
        return costCentreRepository.save(CostCentre.create(tenantId, companyId, code, name, parentId));
    }

    @Transactional
    public CostCentre rename(UUID costCentreId, String name) {
        CostCentre costCentre = get(costCentreId);
        costCentre.rename(name);
        return costCentreRepository.save(costCentre);
    }

    @Transactional
    public void disable(UUID costCentreId) {
        get(costCentreId).disable();
    }

    @Transactional
    public void enable(UUID costCentreId) {
        get(costCentreId).enable();
    }

    @Transactional(readOnly = true)
    public CostCentre get(UUID costCentreId) {
        return costCentreRepository.findById(costCentreId).orElseThrow(() -> new NoSuchElementException("No such cost centre"));
    }

    @Transactional(readOnly = true)
    public List<CostCentre> listForCompany(UUID companyId) {
        return costCentreRepository.findByCompanyId(companyId);
    }
}
