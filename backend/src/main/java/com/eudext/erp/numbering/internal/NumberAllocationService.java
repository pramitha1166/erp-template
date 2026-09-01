package com.eudext.erp.numbering.internal;

import com.eudext.erp.numbering.NoActiveSeriesException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * NUM-2 / NUM-4: gapless, concurrency-safe document number allocation.
 *
 * <p>Gaplessness (NUM-2) comes from running under the caller's own
 * transaction ({@code Propagation.REQUIRED}, the default): once business
 * modules call this from within the same transaction as their document
 * submission (the intended Phase 1+ integration), a rollback of that
 * transaction rolls back this method's counter increment too, so a failed
 * submission never burns a number. Concurrency-safety (NUM-4) comes from
 * {@link NumberingSeriesRepository#findForUpdate}'s pessimistic write lock,
 * which serialises concurrent allocations against the same series row.
 */
@Service
class NumberAllocationService {

    private final NumberingSeriesRepository repository;

    NumberAllocationService(NumberingSeriesRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    String allocate(UUID companyId, String docType, LocalDate onDate) {
        NumberingSeries series =
                repository.findForUpdate(companyId, docType).orElseThrow(() -> new NoActiveSeriesException(companyId, docType));
        long counter = series.allocate(onDate);
        return series.resolvedPrefix(onDate) + SeriesNumberFormatter.zeroPad(counter, series.getCounterWidth());
    }
}
