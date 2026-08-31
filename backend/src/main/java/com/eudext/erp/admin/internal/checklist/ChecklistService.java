package com.eudext.erp.admin.internal.checklist;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADM-4: tracks completion of the fixed post-onboarding checklist.
 * Deliberately manual-only for now — auto-detecting completion (e.g. "a
 * Branch exists" or "a first invoice was submitted") needs hooks into
 * modules that don't exist yet in Phase 0 (Epic 0.9 branches, Phase 1
 * sales); the tenant admin toggles items themselves until those land,
 * exactly the "stock-hook interface point" style deferral CLAUDE.md
 * already establishes for FIN-8.
 */
@Service
public class ChecklistService {

    private final OnboardingChecklistItemRepository repository;

    public ChecklistService(OnboardingChecklistItemRepository repository) {
        this.repository = repository;
    }

    /** No-op if already seeded — runs exactly once, at onboarding. */
    @Transactional
    public void seedDefaults(UUID tenantId) {
        if (repository.existsByTenantId(tenantId)) {
            return;
        }
        for (ChecklistItemKey key : ChecklistItemKey.values()) {
            repository.save(OnboardingChecklistItem.of(tenantId, key));
        }
    }

    @Transactional(readOnly = true)
    public List<OnboardingChecklistItem> list(UUID tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @Transactional
    public void setCompleted(UUID tenantId, ChecklistItemKey itemKey, boolean completed) {
        OnboardingChecklistItem item = repository
                .findByTenantIdAndItemKey(tenantId, itemKey)
                .orElseThrow(() -> new NoSuchElementException("No such checklist item"));
        item.setCompleted(completed);
        repository.save(item);
    }
}
