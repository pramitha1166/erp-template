package com.eudext.erp.masterdata.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.masterdata.internal.item.Item;
import com.eudext.erp.masterdata.internal.item.ItemService;
import com.eudext.erp.masterdata.internal.item.ValuationMethod;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** MDM-6 / MDM-7: item master administration. */
@RestController
@RequestMapping("/masterdata/items")
public class ItemController {

    private static final String PERMISSION_MANAGE = "masterdata:item:manage";
    private static final String PERMISSION_VIEW = "masterdata:item:view";

    private final ItemService itemService;
    private final MasterDataAccessControl accessControl;

    public ItemController(ItemService itemService, MasterDataAccessControl accessControl) {
        this.itemService = itemService;
        this.accessControl = accessControl;
    }

    public record NewItemRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotNull UUID itemGroupId,
            @NotNull UUID stockUomId,
            @NotNull ValuationMethod valuationMethod) {}

    public record UpdateItemRequest(
            @NotBlank String name,
            @NotNull UUID itemGroupId,
            UUID purchaseUomId,
            @NotNull ValuationMethod valuationMethod,
            @NotNull @DecimalMin("0") BigDecimal reorderLevel,
            boolean batchTracked,
            boolean serialTracked,
            String taxCategoryCode,
            String hsCode) {}

    public record ItemView(
            UUID id,
            String code,
            String name,
            UUID itemGroupId,
            UUID stockUomId,
            UUID purchaseUomId,
            ValuationMethod valuationMethod,
            BigDecimal reorderLevel,
            boolean batchTracked,
            boolean serialTracked,
            String taxCategoryCode,
            String hsCode,
            boolean disabled) {
        static ItemView from(Item item) {
            return new ItemView(
                    item.getId(),
                    item.getCode(),
                    item.getName(),
                    item.getItemGroupId(),
                    item.getStockUomId(),
                    item.getPurchaseUomId(),
                    item.getValuationMethod(),
                    item.getReorderLevel(),
                    item.isBatchTracked(),
                    item.isSerialTracked(),
                    item.getTaxCategoryCode(),
                    item.getHsCode(),
                    item.isDisabled());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemView create(@RequestParam UUID companyId, @Valid @RequestBody NewItemRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        Item item = itemService.create(
                tenantId(), companyId, request.code(), request.name(), request.itemGroupId(), request.stockUomId(),
                request.valuationMethod());
        return ItemView.from(item);
    }

    @GetMapping
    public List<ItemView> list(@RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_VIEW);
        return itemService.listForCompany(companyId).stream().map(ItemView::from).toList();
    }

    @PutMapping("/{itemId}")
    public ItemView update(@PathVariable UUID itemId, @RequestParam UUID companyId, @Valid @RequestBody UpdateItemRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        Item item = itemService.update(
                itemId,
                request.name(),
                request.itemGroupId(),
                request.purchaseUomId(),
                request.valuationMethod(),
                request.reorderLevel(),
                request.batchTracked(),
                request.serialTracked(),
                request.taxCategoryCode(),
                request.hsCode());
        return ItemView.from(item);
    }

    @PostMapping("/{itemId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable UUID itemId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        itemService.disable(itemId);
    }

    @PostMapping("/{itemId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable UUID itemId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        itemService.enable(itemId);
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
