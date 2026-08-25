package com.eudext.erp.admin.internal.checklist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingChecklistItemRepository extends JpaRepository<OnboardingChecklistItem, UUID> {

    List<OnboardingChecklistItem> findByTenantId(UUID tenantId);

    Optional<OnboardingChecklistItem> findByTenantIdAndItemKey(UUID tenantId, ChecklistItemKey itemKey);

    boolean existsByTenantId(UUID tenantId);
}
