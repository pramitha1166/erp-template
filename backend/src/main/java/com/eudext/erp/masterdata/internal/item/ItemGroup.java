package com.eudext.erp.masterdata.internal.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** MDM-6: a hierarchical grouping items are classified under. */
@Entity
@Table(name = "item_groups")
@EntityListeners(AuditingEntityListener.class)
public class ItemGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "code", nullable = false, updatable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "disabled", nullable = false)
    private boolean disabled;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedBy
    @Column(name = "modified_by")
    private String modifiedBy;

    @LastModifiedDate
    @Column(name = "modified_at")
    private Instant modifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ItemGroup() {}

    public static ItemGroup create(UUID tenantId, UUID companyId, String code, String name, UUID parentId) {
        ItemGroup group = new ItemGroup();
        group.tenantId = tenantId;
        group.companyId = companyId;
        group.code = code;
        group.name = name;
        group.parentId = parentId;
        return group;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public UUID getParentId() {
        return parentId;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void rename(String name) {
        this.name = name;
    }

    /** MDM-10: soft-delete only — an ItemGroup is never hard-deleted once it may be referenced. */
    public void disable() {
        this.disabled = true;
    }

    public void enable() {
        this.disabled = false;
    }
}
