package com.eudext.erp.numbering.internal;

import com.eudext.erp.numbering.NumberingApi;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class NumberingApiImpl implements NumberingApi {

    private final NumberingSeedService seedService;

    NumberingApiImpl(NumberingSeedService seedService) {
        this.seedService = seedService;
    }

    @Override
    public void seedDefaultSeries(UUID tenantId, UUID companyId) {
        seedService.seedDefaults(tenantId, companyId);
    }
}
