package com.eudext.erp.masterdata.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.masterdata.internal.uom.UnitOfMeasure;
import com.eudext.erp.masterdata.internal.uom.UomConversion;
import com.eudext.erp.masterdata.internal.uom.UomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * MDM-7: unit-of-measure administration and conversion factors. UOMs are shared across a tenant's companies, so
 * mutating operations anchor their permission check on an {@code authorizingCompanyId} the caller manages.
 */
@RestController
@RequestMapping("/masterdata/uoms")
public class UomController {

    private static final String PERMISSION_MANAGE = "masterdata:uom:manage";

    private final UomService uomService;
    private final MasterDataAccessControl accessControl;

    public UomController(UomService uomService, MasterDataAccessControl accessControl) {
        this.uomService = uomService;
        this.accessControl = accessControl;
    }

    public record NewUomRequest(@NotBlank String code, @NotBlank String name) {}

    public record ConfigureConversionRequest(
            @NotNull UUID fromUomId, @NotNull UUID toUomId, @NotNull @DecimalMin("0.000001") BigDecimal conversionFactor) {}

    public record UomView(UUID id, String code, String name, boolean disabled) {
        static UomView from(UnitOfMeasure uom) {
            return new UomView(uom.getId(), uom.getCode(), uom.getName(), uom.isDisabled());
        }
    }

    public record ConversionView(UUID id, UUID fromUomId, UUID toUomId, BigDecimal conversionFactor) {
        static ConversionView from(UomConversion conversion) {
            return new ConversionView(
                    conversion.getId(), conversion.getFromUomId(), conversion.getToUomId(), conversion.getConversionFactor());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UomView create(@RequestParam UUID authorizingCompanyId, @Valid @RequestBody NewUomRequest request) {
        accessControl.requirePermission(authorizingCompanyId, PERMISSION_MANAGE);
        return UomView.from(uomService.create(tenantId(), request.code(), request.name()));
    }

    /** Every UOM belonging to the caller's own tenant — RLS already scopes this, no further check needed. */
    @GetMapping
    public List<UomView> list() {
        accessControl.currentUserId();
        return uomService.listForTenant(tenantId()).stream().map(UomView::from).toList();
    }

    @PostMapping("/{uomId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable UUID uomId, @RequestParam UUID authorizingCompanyId) {
        accessControl.requirePermission(authorizingCompanyId, PERMISSION_MANAGE);
        uomService.disable(uomId);
    }

    @PostMapping("/{uomId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable UUID uomId, @RequestParam UUID authorizingCompanyId) {
        accessControl.requirePermission(authorizingCompanyId, PERMISSION_MANAGE);
        uomService.enable(uomId);
    }

    @PostMapping("/conversions")
    @ResponseStatus(HttpStatus.OK)
    public ConversionView configureConversion(
            @RequestParam UUID authorizingCompanyId, @Valid @RequestBody ConfigureConversionRequest request) {
        accessControl.requirePermission(authorizingCompanyId, PERMISSION_MANAGE);
        return ConversionView.from(
                uomService.configureConversion(tenantId(), request.fromUomId(), request.toUomId(), request.conversionFactor()));
    }

    @GetMapping("/{uomId}/conversions")
    public List<ConversionView> conversionsFrom(@PathVariable UUID uomId) {
        accessControl.currentUserId();
        return uomService.conversionsFrom(uomId).stream().map(ConversionView::from).toList();
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
