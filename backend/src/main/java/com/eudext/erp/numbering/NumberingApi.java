package com.eudext.erp.numbering;

import java.time.LocalDate;
import java.util.UUID;

/**
 * NUM-1 / ADM-3: seeds a company's default naming series at onboarding
 * time, and NUM-2/NUM-4: the entry point document-owning modules use to
 * obtain their next document number without reaching into numbering's
 * internal tables (ARCH-1).
 */
public interface NumberingApi {

    void seedDefaultSeries(UUID tenantId, UUID companyId);

    /**
     * Allocates and returns the next formatted document number for (companyId, docType). Call this from within the
     * same transaction as the document's own submission so a rollback rolls back the allocation too (NUM-2
     * gaplessness) — this method itself only requires a transaction to exist, joining the caller's if present.
     *
     * @throws NoActiveSeriesException if no active series is configured for (companyId, docType)
     */
    String allocateNumber(UUID companyId, String docType, LocalDate onDate);
}
