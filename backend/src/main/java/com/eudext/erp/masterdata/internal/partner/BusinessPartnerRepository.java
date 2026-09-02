package com.eudext.erp.masterdata.internal.partner;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessPartnerRepository extends JpaRepository<BusinessPartner, UUID> {

    List<BusinessPartner> findByCompanyId(UUID companyId);

    List<BusinessPartner> findByCompanyIdAndPartnerType(UUID companyId, BusinessPartnerType partnerType);
}
