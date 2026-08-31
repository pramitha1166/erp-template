package com.eudext.erp.admin.internal.checklist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** ADM-4: one step of a tenant's post-onboarding guided setup checklist. Tenant-owned data — RLS applies. */
@Entity
@Table(name = "onboarding_checklist_items")
public class OnboardingChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_key", nullable = false, updatable = false)
    private ChecklistItemKey itemKey;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected OnboardingChecklistItem() {}

    public static OnboardingChecklistItem of(UUID tenantId, ChecklistItemKey itemKey) {
        OnboardingChecklistItem item = new OnboardingChecklistItem();
        item.tenantId = tenantId;
        item.itemKey = itemKey;
        return item;
    }

    public UUID getId() {
        return id;
    }

    public ChecklistItemKey getItemKey() {
        return itemKey;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        this.completedAt = completed ? Instant.now() : null;
    }
}
