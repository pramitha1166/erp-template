package com.eudext.erp.masterdata.internal.item;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemGroupRepository extends JpaRepository<ItemGroup, UUID> {

    List<ItemGroup> findByCompanyId(UUID companyId);
}
