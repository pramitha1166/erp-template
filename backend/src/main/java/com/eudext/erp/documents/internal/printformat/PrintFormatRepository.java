package com.eudext.erp.documents.internal.printformat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrintFormatRepository extends JpaRepository<PrintFormat, UUID> {

    List<PrintFormat> findByCompanyIdAndDocumentType(UUID companyId, String documentType);

    Optional<PrintFormat> findByCompanyIdAndDocumentTypeAndIsDefaultTrueAndDisabledFalse(UUID companyId, String documentType);
}
