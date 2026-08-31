package com.eudext.erp.numbering.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NumberingSeriesRepository extends JpaRepository<NumberingSeries, UUID> {

    List<NumberingSeries> findByCompanyId(UUID companyId);

    boolean existsByCompanyId(UUID companyId);
}
