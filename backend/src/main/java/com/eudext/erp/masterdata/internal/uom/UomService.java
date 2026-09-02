package com.eudext.erp.masterdata.internal.uom;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** MDM-7: units of measure and the conversion factors between them. */
@Service
public class UomService {

    private final UnitOfMeasureRepository uomRepository;
    private final UomConversionRepository conversionRepository;

    public UomService(UnitOfMeasureRepository uomRepository, UomConversionRepository conversionRepository) {
        this.uomRepository = uomRepository;
        this.conversionRepository = conversionRepository;
    }

    @Transactional
    public UnitOfMeasure create(UUID tenantId, String code, String name) {
        return uomRepository.save(UnitOfMeasure.create(tenantId, code, name));
    }

    @Transactional
    public void disable(UUID uomId) {
        get(uomId).disable();
    }

    @Transactional
    public void enable(UUID uomId) {
        get(uomId).enable();
    }

    @Transactional(readOnly = true)
    public UnitOfMeasure get(UUID uomId) {
        return uomRepository.findById(uomId).orElseThrow(() -> new NoSuchElementException("No such unit of measure"));
    }

    @Transactional(readOnly = true)
    public List<UnitOfMeasure> listForTenant(UUID tenantId) {
        return uomRepository.findByTenantId(tenantId);
    }

    /** MDM-7: configures (or reconfigures) the conversion factor from one UOM to another. */
    @Transactional
    public UomConversion configureConversion(UUID tenantId, UUID fromUomId, UUID toUomId, BigDecimal conversionFactor) {
        get(fromUomId);
        get(toUomId);
        return conversionRepository
                .findByFromUomIdAndToUomId(fromUomId, toUomId)
                .map(existing -> {
                    existing.updateFactor(conversionFactor);
                    return existing;
                })
                .orElseGet(() -> conversionRepository.save(UomConversion.create(tenantId, fromUomId, toUomId, conversionFactor)));
    }

    @Transactional(readOnly = true)
    public List<UomConversion> conversionsFrom(UUID uomId) {
        return conversionRepository.findByFromUomId(uomId);
    }
}
