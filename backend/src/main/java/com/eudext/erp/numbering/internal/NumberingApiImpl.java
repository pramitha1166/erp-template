package com.eudext.erp.numbering.internal;

import com.eudext.erp.numbering.NumberingApi;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class NumberingApiImpl implements NumberingApi {

    private final NumberingSeedService seedService;
    private final NumberAllocationService allocationService;

    NumberingApiImpl(NumberingSeedService seedService, NumberAllocationService allocationService) {
        this.seedService = seedService;
        this.allocationService = allocationService;
    }

    @Override
    public void seedDefaultSeries(UUID tenantId, UUID companyId) {
        seedService.seedDefaults(tenantId, companyId);
    }

    @Override
    public String allocateNumber(UUID companyId, String docType, LocalDate onDate) {
        return allocationService.allocate(companyId, docType, onDate);
    }
}
