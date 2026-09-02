package com.eudext.erp.masterdata.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.masterdata.internal.partner.BusinessPartner;
import com.eudext.erp.masterdata.internal.partner.BusinessPartnerContact;
import com.eudext.erp.masterdata.internal.partner.BusinessPartnerService;
import com.eudext.erp.masterdata.internal.partner.BusinessPartnerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** MDM-5: customer/supplier master administration, including contact persons. */
@RestController
@RequestMapping("/masterdata/business-partners")
public class BusinessPartnerController {

    private static final String PERMISSION_MANAGE = "masterdata:partner:manage";
    private static final String PERMISSION_VIEW = "masterdata:partner:view";

    private final BusinessPartnerService partnerService;
    private final MasterDataAccessControl accessControl;

    public BusinessPartnerController(BusinessPartnerService partnerService, MasterDataAccessControl accessControl) {
        this.partnerService = partnerService;
        this.accessControl = accessControl;
    }

    public record NewPartnerRequest(@NotNull BusinessPartnerType partnerType, @NotBlank String code, @NotBlank String name) {}

    public record UpdatePartnerRequest(
            @NotBlank String name,
            String taxRegistrationNo,
            @NotNull BigDecimal creditLimit,
            @Min(0) int creditTermsDays,
            UUID defaultAccountId,
            String bankName,
            String bankBranch,
            String bankAccountNo,
            String bankSwiftCode) {}

    public record NewContactRequest(
            @NotBlank String name, String designation, String phone, @Email String email, boolean primaryContact) {}

    public record PartnerView(
            UUID id,
            BusinessPartnerType partnerType,
            String code,
            String name,
            String taxRegistrationNo,
            BigDecimal creditLimit,
            int creditTermsDays,
            UUID defaultAccountId,
            String bankName,
            String bankBranch,
            String bankAccountNo,
            String bankSwiftCode,
            boolean disabled) {
        static PartnerView from(BusinessPartner partner) {
            return new PartnerView(
                    partner.getId(),
                    partner.getPartnerType(),
                    partner.getCode(),
                    partner.getName(),
                    partner.getTaxRegistrationNo(),
                    partner.getCreditLimit(),
                    partner.getCreditTermsDays(),
                    partner.getDefaultAccountId(),
                    partner.getBankName(),
                    partner.getBankBranch(),
                    partner.getBankAccountNo(),
                    partner.getBankSwiftCode(),
                    partner.isDisabled());
        }
    }

    public record ContactView(UUID id, String name, String designation, String phone, String email, boolean primaryContact) {
        static ContactView from(BusinessPartnerContact contact) {
            return new ContactView(
                    contact.getId(), contact.getName(), contact.getDesignation(), contact.getPhone(), contact.getEmail(),
                    contact.isPrimaryContact());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartnerView create(@RequestParam UUID companyId, @Valid @RequestBody NewPartnerRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        return PartnerView.from(
                partnerService.create(tenantId(), companyId, request.partnerType(), request.code(), request.name()));
    }

    @GetMapping
    public List<PartnerView> list(@RequestParam UUID companyId, @RequestParam(required = false) BusinessPartnerType partnerType) {
        accessControl.requirePermission(companyId, PERMISSION_VIEW);
        return partnerService.listForCompany(companyId, partnerType).stream().map(PartnerView::from).toList();
    }

    @PutMapping("/{partnerId}")
    public PartnerView update(
            @PathVariable UUID partnerId, @RequestParam UUID companyId, @Valid @RequestBody UpdatePartnerRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        return PartnerView.from(partnerService.update(
                partnerId,
                request.name(),
                request.taxRegistrationNo(),
                request.creditLimit(),
                request.creditTermsDays(),
                request.defaultAccountId(),
                request.bankName(),
                request.bankBranch(),
                request.bankAccountNo(),
                request.bankSwiftCode()));
    }

    @PostMapping("/{partnerId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable UUID partnerId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        partnerService.disable(partnerId);
    }

    @PostMapping("/{partnerId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable UUID partnerId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        partnerService.enable(partnerId);
    }

    @PostMapping("/{partnerId}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactView addContact(
            @PathVariable UUID partnerId, @RequestParam UUID companyId, @Valid @RequestBody NewContactRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        return ContactView.from(partnerService.addContact(
                partnerId, request.name(), request.designation(), request.phone(), request.email(), request.primaryContact()));
    }

    @GetMapping("/{partnerId}/contacts")
    public List<ContactView> listContacts(@PathVariable UUID partnerId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_VIEW);
        return partnerService.listContacts(partnerId).stream().map(ContactView::from).toList();
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
