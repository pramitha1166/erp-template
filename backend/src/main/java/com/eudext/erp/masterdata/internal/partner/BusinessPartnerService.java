package com.eudext.erp.masterdata.internal.partner;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** MDM-5: customer/supplier master CRUD, plus their contact persons. */
@Service
public class BusinessPartnerService {

    private final BusinessPartnerRepository partnerRepository;
    private final BusinessPartnerContactRepository contactRepository;

    public BusinessPartnerService(
            BusinessPartnerRepository partnerRepository, BusinessPartnerContactRepository contactRepository) {
        this.partnerRepository = partnerRepository;
        this.contactRepository = contactRepository;
    }

    @Transactional
    public BusinessPartner create(UUID tenantId, UUID companyId, BusinessPartnerType partnerType, String code, String name) {
        return partnerRepository.save(BusinessPartner.create(tenantId, companyId, partnerType, code, name));
    }

    @Transactional
    public BusinessPartner update(
            UUID partnerId,
            String name,
            String taxRegistrationNo,
            BigDecimal creditLimit,
            int creditTermsDays,
            UUID defaultAccountId,
            String bankName,
            String bankBranch,
            String bankAccountNo,
            String bankSwiftCode) {
        BusinessPartner partner = get(partnerId);
        partner.updateDetails(
                name, taxRegistrationNo, creditLimit, creditTermsDays, defaultAccountId, bankName, bankBranch, bankAccountNo,
                bankSwiftCode);
        return partnerRepository.save(partner);
    }

    @Transactional
    public void disable(UUID partnerId) {
        get(partnerId).disable();
    }

    @Transactional
    public void enable(UUID partnerId) {
        get(partnerId).enable();
    }

    @Transactional(readOnly = true)
    public BusinessPartner get(UUID partnerId) {
        return partnerRepository.findById(partnerId).orElseThrow(() -> new NoSuchElementException("No such business partner"));
    }

    @Transactional(readOnly = true)
    public List<BusinessPartner> listForCompany(UUID companyId, BusinessPartnerType partnerType) {
        return partnerType == null
                ? partnerRepository.findByCompanyId(companyId)
                : partnerRepository.findByCompanyIdAndPartnerType(companyId, partnerType);
    }

    @Transactional
    public BusinessPartnerContact addContact(
            UUID partnerId, String name, String designation, String phone, String email, boolean primaryContact) {
        BusinessPartner partner = get(partnerId);
        return contactRepository.save(
                BusinessPartnerContact.create(partner.getTenantId(), partnerId, name, designation, phone, email, primaryContact));
    }

    @Transactional(readOnly = true)
    public List<BusinessPartnerContact> listContacts(UUID partnerId) {
        return contactRepository.findByPartnerId(partnerId);
    }
}
