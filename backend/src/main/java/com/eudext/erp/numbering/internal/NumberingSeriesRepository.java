package com.eudext.erp.numbering.internal;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface NumberingSeriesRepository extends JpaRepository<NumberingSeries, UUID> {

    List<NumberingSeries> findByCompanyId(UUID companyId);

    Optional<NumberingSeries> findByCompanyIdAndDocType(UUID companyId, String docType);

    boolean existsByCompanyId(UUID companyId);

    /**
     * NUM-4: locks the series row for the duration of the caller's transaction, serialising concurrent
     * allocation attempts against the same (companyId, docType) so counter increments are never lost.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from NumberingSeries s where s.companyId = :companyId and s.docType = :docType and s.active = true")
    Optional<NumberingSeries> findForUpdate(UUID companyId, String docType);
}
