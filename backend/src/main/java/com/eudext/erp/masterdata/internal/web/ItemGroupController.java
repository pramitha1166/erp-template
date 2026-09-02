package com.eudext.erp.masterdata.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.masterdata.internal.item.ItemGroup;
import com.eudext.erp.masterdata.internal.item.ItemGroupService;
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

/** MDM-6: hierarchical item group administration. */
@RestController
@RequestMapping("/masterdata/item-groups")
public class ItemGroupController {

    private static final String PERMISSION_MANAGE = "masterdata:item:manage";
    private static final String PERMISSION_VIEW = "masterdata:item:view";

    private final ItemGroupService itemGroupService;
    private final MasterDataAccessControl accessControl;

    public ItemGroupController(ItemGroupService itemGroupService, MasterDataAccessControl accessControl) {
        this.itemGroupService = itemGroupService;
        this.accessControl = accessControl;
    }

    public record NewItemGroupRequest(@NotBlank String code, @NotBlank String name, UUID parentId) {}

    public record RenameItemGroupRequest(@NotBlank String name) {}

    public record ItemGroupView(UUID id, String code, String name, UUID parentId, boolean disabled) {
        static ItemGroupView from(ItemGroup group) {
            return new ItemGroupView(group.getId(), group.getCode(), group.getName(), group.getParentId(), group.isDisabled());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemGroupView create(@RequestParam UUID companyId, @Valid @RequestBody NewItemGroupRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        return ItemGroupView.from(itemGroupService.create(tenantId(), companyId, request.code(), request.name(), request.parentId()));
    }

    @GetMapping
    public List<ItemGroupView> list(@RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_VIEW);
        return itemGroupService.listForCompany(companyId).stream().map(ItemGroupView::from).toList();
    }

    @PutMapping("/{itemGroupId}")
    public ItemGroupView rename(
            @PathVariable UUID itemGroupId, @RequestParam UUID companyId, @Valid @RequestBody RenameItemGroupRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        return ItemGroupView.from(itemGroupService.rename(itemGroupId, request.name()));
    }

    @PostMapping("/{itemGroupId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable UUID itemGroupId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        itemGroupService.disable(itemGroupId);
    }

    @PostMapping("/{itemGroupId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable UUID itemGroupId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        itemGroupService.enable(itemGroupId);
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
