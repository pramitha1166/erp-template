package com.eudext.erp.masterdata.internal.uom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UomConversionRepository extends JpaRepository<UomConversion, UUID> {

    List<UomConversion> findByFromUomId(UUID fromUomId);

    Optional<UomConversion> findByFromUomIdAndToUomId(UUID fromUomId, UUID toUomId);
}
