package com.eudext.erp.numbering.internal;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class NumberingSeedService {

    private final NumberingSeriesRepository repository;

    NumberingSeedService(NumberingSeriesRepository repository) {
        this.repository = repository;
    }

    /** No-op if the company already has series configured — seeding runs exactly once, at onboarding. */
    @Transactional
    void seedDefaults(UUID tenantId, UUID companyId) {
        if (repository.existsByCompanyId(companyId)) {
            return;
        }
        for (DefaultSeriesTemplate.Entry entry : DefaultSeriesTemplate.standard()) {
            repository.save(NumberingSeries.create(tenantId, companyId, entry.docType(), entry.prefix(), entry.counterWidth()));
        }
    }
}
