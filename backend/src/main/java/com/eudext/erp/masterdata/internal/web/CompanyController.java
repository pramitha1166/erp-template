package com.eudext.erp.masterdata.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.masterdata.internal.company.Company;
import com.eudext.erp.masterdata.internal.company.CompanyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

/**
 * MDM-1 / MDM-2: master-data administration surface for companies. Onboarding's first company still comes from
 * {@code MasterDataProvisioningApi} (Epic 0.11) — this is where a tenant manages that company afterwards and, per
 * MDM-2, adds further companies of its own.
 */
@RestController
@RequestMapping("/masterdata/companies")
public class CompanyController {

    private static final String PERMISSION_MANAGE = "masterdata:company:manage";

    private final CompanyService companyService;
    private final MasterDataAccessControl accessControl;

    public CompanyController(CompanyService companyService, MasterDataAccessControl accessControl) {
        this.companyService = companyService;
        this.accessControl = accessControl;
    }

    public record NewCompanyRequest(
            @NotBlank String legalName,
            String registrationNo,
            String vatNo,
            String address,
            @NotBlank String baseCurrency,
            @Min(1) int fiscalYearStartMonth) {}

    public record UpdateCompanyRequest(@NotBlank String legalName, String address, String logoUrl) {}

    public record CompanyView(
            UUID id,
            String legalName,
            String registrationNo,
            String vatNo,
            String address,
            String baseCurrency,
            int fiscalYearStartMonth,
            String logoUrl,
            boolean disabled) {
        static CompanyView from(Company company) {
            return new CompanyView(
                    company.getId(),
                    company.getLegalName(),
                    company.getRegistrationNo(),
                    company.getVatNo(),
                    company.getAddress(),
                    company.getBaseCurrency(),
                    company.getFiscalYearStartMonth(),
                    company.getLogoUrl(),
                    company.isDisabled());
        }
    }

    /** MDM-2: adding a further company to the tenant. {@code authorizingCompanyId} anchors the permission check. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyView create(@RequestParam UUID authorizingCompanyId, @Valid @RequestBody NewCompanyRequest request) {
        accessControl.requirePermission(authorizingCompanyId, PERMISSION_MANAGE);
        Company company = companyService.create(
                tenantId(),
                request.legalName(),
                request.registrationNo(),
                request.vatNo(),
                request.address(),
                request.baseCurrency(),
                request.fiscalYearStartMonth());
        return CompanyView.from(company);
    }

    /** Every company belonging to the caller's own tenant — RLS already scopes this, no further check needed. */
    @GetMapping
    public List<CompanyView> list() {
        accessControl.currentUserId();
        return companyService.listForTenant(tenantId()).stream().map(CompanyView::from).toList();
    }

    @GetMapping("/{companyId}")
    public CompanyView get(@PathVariable UUID companyId) {
        accessControl.currentUserId();
        return CompanyView.from(companyService.get(companyId));
    }

    @PutMapping("/{companyId}")
    public CompanyView update(@PathVariable UUID companyId, @Valid @RequestBody UpdateCompanyRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        return CompanyView.from(companyService.update(companyId, request.legalName(), request.address(), request.logoUrl()));
    }

    @PostMapping("/{companyId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        companyService.disable(companyId);
    }

    @PostMapping("/{companyId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        companyService.enable(companyId);
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
