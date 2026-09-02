package com.eudext.erp.masterdata.internal.item;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** MDM-6: item group CRUD, hierarchical via {@code parentId}. */
@Service
public class ItemGroupService {

    private final ItemGroupRepository itemGroupRepository;

    public ItemGroupService(ItemGroupRepository itemGroupRepository) {
        this.itemGroupRepository = itemGroupRepository;
    }

    @Transactional
    public ItemGroup create(UUID tenantId, UUID companyId, String code, String name, UUID parentId) {
        if (parentId != null && !itemGroupRepository.existsById(parentId)) {
            throw new NoSuchElementException("No such parent item group");
        }
        return itemGroupRepository.save(ItemGroup.create(tenantId, companyId, code, name, parentId));
    }

    @Transactional
    public ItemGroup rename(UUID itemGroupId, String name) {
        ItemGroup group = get(itemGroupId);
        group.rename(name);
        return itemGroupRepository.save(group);
    }

    @Transactional
    public void disable(UUID itemGroupId) {
        get(itemGroupId).disable();
    }

    @Transactional
    public void enable(UUID itemGroupId) {
        get(itemGroupId).enable();
    }

    @Transactional(readOnly = true)
    public ItemGroup get(UUID itemGroupId) {
        return itemGroupRepository.findById(itemGroupId).orElseThrow(() -> new NoSuchElementException("No such item group"));
    }

    @Transactional(readOnly = true)
    public List<ItemGroup> listForCompany(UUID companyId) {
        return itemGroupRepository.findByCompanyId(companyId);
    }
}
