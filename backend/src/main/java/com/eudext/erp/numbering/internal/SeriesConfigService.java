package com.eudext.erp.numbering.internal;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** NUM-1: naming-series configuration — create/update, list, and lifecycle for a company's series. */
@Service
public class SeriesConfigService {

    private final NumberingSeriesRepository repository;

    SeriesConfigService(NumberingSeriesRepository repository) {
        this.repository = repository;
    }

    /** Creates the series for (companyId, docType) if it doesn't exist yet, otherwise reconfigures the existing one. */
    @Transactional
    public NumberingSeries configure(
            UUID tenantId,
            UUID companyId,
            String docType,
            String prefix,
            int counterWidth,
            NumberingResetPolicy resetPolicy,
            int fiscalYearStartMonth) {
        NumberingSeries series = repository
                .findByCompanyIdAndDocType(companyId, docType)
                .orElseGet(() -> NumberingSeries.create(tenantId, companyId, docType, prefix, counterWidth));
        series.configure(prefix, counterWidth, resetPolicy, fiscalYearStartMonth);
        return repository.save(series);
    }

    @Transactional
    public void activate(UUID seriesId) {
        getSeries(seriesId).activate();
    }

    @Transactional
    public void deactivate(UUID seriesId) {
        getSeries(seriesId).deactivate();
    }

    @Transactional(readOnly = true)
    public NumberingSeries getSeries(UUID seriesId) {
        return repository.findById(seriesId).orElseThrow(() -> new NoSuchElementException("No such numbering series"));
    }

    @Transactional(readOnly = true)
    public List<NumberingSeries> seriesFor(UUID companyId) {
        return repository.findByCompanyId(companyId);
    }
}
