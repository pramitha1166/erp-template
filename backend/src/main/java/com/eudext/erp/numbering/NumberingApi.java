package com.eudext.erp.numbering;

import java.util.UUID;

/**
 * NUM-1 / ADM-3: seeds a company's default naming series at onboarding
 * time. Allocation (NUM-2 gapless sequencing, NUM-4 concurrency-safety) is
 * Epic 0.5's own scope and not exposed here yet.
 */
public interface NumberingApi {

    void seedDefaultSeries(UUID tenantId, UUID companyId);
}
