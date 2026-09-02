package com.eudext.erp.masterdata.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.masterdata.internal.costcentre.CostCentre;
import com.eudext.erp.masterdata.internal.costcentre.CostCentreService;
import jakarta.validation.Valid;
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

/** MDM-4: hierarchical cost centre administration. */
@RestController
@RequestMapping("/masterdata/cost-centres")
public class CostCentreController {

    private static final String PERMISSION_MANAGE = "masterdata:costcentre:manage";
    private static final String PERMISSION_VIEW = "masterdata:costcentre:view";

    private final CostCentreService costCentreService;
    private final MasterDataAccessControl accessControl;

    public CostCentreController(CostCentreService costCentreService, MasterDataAccessControl accessControl) {
        this.costCentreService = costCentreService;
        this.accessControl = accessControl;
    }

    public record NewCostCentreRequest(@NotBlank String code, @NotBlank String name, UUID parentId) {}

    public record RenameCostCentreRequest(@NotBlank String name) {}

    public record CostCentreView(UUID id, String code, String name, UUID parentId, boolean disabled) {
        static CostCentreView from(CostCentre costCentre) {
            return new CostCentreView(
                    costCentre.getId(), costCentre.getCode(), costCentre.getName(), costCentre.getParentId(), costCentre.isDisabled());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CostCentreView create(@RequestParam UUID companyId, @Valid @RequestBody NewCostCentreRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        return CostCentreView.from(
                costCentreService.create(tenantId(), companyId, request.code(), request.name(), request.parentId()));
    }

    @GetMapping
    public List<CostCentreView> list(@RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_VIEW);
        return costCentreService.listForCompany(companyId).stream().map(CostCentreView::from).toList();
    }

    @PutMapping("/{costCentreId}")
    public CostCentreView rename(
            @PathVariable UUID costCentreId, @RequestParam UUID companyId, @Valid @RequestBody RenameCostCentreRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        return CostCentreView.from(costCentreService.rename(costCentreId, request.name()));
    }

    @PostMapping("/{costCentreId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable UUID costCentreId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        costCentreService.disable(costCentreId);
    }

    @PostMapping("/{costCentreId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable UUID costCentreId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        costCentreService.enable(costCentreId);
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
