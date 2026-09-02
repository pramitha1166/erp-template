package com.eudext.erp.masterdata.internal.item;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** MDM-6: item master CRUD. */
@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemGroupRepository itemGroupRepository;

    public ItemService(ItemRepository itemRepository, ItemGroupRepository itemGroupRepository) {
        this.itemRepository = itemRepository;
        this.itemGroupRepository = itemGroupRepository;
    }

    @Transactional
    public Item create(
            UUID tenantId, UUID companyId, String code, String name, UUID itemGroupId, UUID stockUomId, ValuationMethod valuationMethod) {
        requireItemGroup(itemGroupId);
        return itemRepository.save(Item.create(tenantId, companyId, code, name, itemGroupId, stockUomId, valuationMethod));
    }

    @Transactional
    public Item update(
            UUID itemId,
            String name,
            UUID itemGroupId,
            UUID purchaseUomId,
            ValuationMethod valuationMethod,
            BigDecimal reorderLevel,
            boolean batchTracked,
            boolean serialTracked,
            String taxCategoryCode,
            String hsCode) {
        requireItemGroup(itemGroupId);
        Item item = get(itemId);
        item.updateDetails(
                name, itemGroupId, purchaseUomId, valuationMethod, reorderLevel, batchTracked, serialTracked, taxCategoryCode, hsCode);
        return itemRepository.save(item);
    }

    @Transactional
    public void disable(UUID itemId) {
        get(itemId).disable();
    }

    @Transactional
    public void enable(UUID itemId) {
        get(itemId).enable();
    }

    @Transactional(readOnly = true)
    public Item get(UUID itemId) {
        return itemRepository.findById(itemId).orElseThrow(() -> new NoSuchElementException("No such item"));
    }

    @Transactional(readOnly = true)
    public List<Item> listForCompany(UUID companyId) {
        return itemRepository.findByCompanyId(companyId);
    }

    private void requireItemGroup(UUID itemGroupId) {
        if (!itemGroupRepository.existsById(itemGroupId)) {
            throw new NoSuchElementException("No such item group");
        }
    }
}
