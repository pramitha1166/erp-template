package com.eudext.erp.masterdata.internal.coa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * MDM-3: one node of a company's Chart of Accounts. A minimal, flat-tree
 * shape (code/name/type/parent/group-vs-ledger) — the full hierarchical CoA
 * management surface (multi-currency accounts, account-level restrictions,
 * etc.) is Epic 0.6's own scope; this is only what ADM-3's onboarding seed
 * needs to leave a tenant transaction-ready.
 */
@Entity
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener.class)
public class Account {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, updatable = false)
    private AccountType accountType;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "is_group", nullable = false)
    private boolean group;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Account() {}

    public static Account create(
            UUID tenantId, UUID companyId, String code, String name, AccountType accountType, UUID parentId, boolean group) {
        Account account = new Account();
        account.tenantId = tenantId;
        account.companyId = companyId;
        account.code = code;
        account.name = name;
        account.accountType = accountType;
        account.parentId = parentId;
        account.group = group;
        return account;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public UUID getParentId() {
        return parentId;
    }

    public boolean isGroup() {
        return group;
    }

    public boolean isActive() {
        return active;
    }

    public void rename(String name) {
        this.name = name;
    }

    /** MDM-10: soft-delete only — an Account is never hard-deleted once it may be referenced. */
    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
