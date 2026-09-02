package com.eudext.erp.masterdata.internal.partner;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessPartnerContactRepository extends JpaRepository<BusinessPartnerContact, UUID> {

    List<BusinessPartnerContact> findByPartnerId(UUID partnerId);
}
