package com.eudext.erp.masterdata.internal.coa;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MDM-3: Chart of Accounts CRUD beyond the onboarding seed (see {@link ChartOfAccountsSeedService}) — creating and
 * renaming nodes, and enforcing the group-vs-ledger tree invariant: only a group account may have children, and a
 * new account's parent (if any) must itself be a group account.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account create(
            UUID tenantId, UUID companyId, String code, String name, AccountType accountType, UUID parentId, boolean group) {
        Account parent = requireGroupParent(parentId);
        if (parent != null && parent.getAccountType() != accountType) {
            throw new IllegalArgumentException("A child account must share its parent's account type");
        }
        return accountRepository.save(Account.create(tenantId, companyId, code, name, accountType, parentId, group));
    }

    @Transactional
    public Account rename(UUID accountId, String name) {
        Account account = get(accountId);
        account.rename(name);
        return accountRepository.save(account);
    }

    @Transactional
    public void deactivate(UUID accountId) {
        get(accountId).deactivate();
    }

    @Transactional
    public void activate(UUID accountId) {
        get(accountId).activate();
    }

    @Transactional(readOnly = true)
    public Account get(UUID accountId) {
        return accountRepository.findById(accountId).orElseThrow(() -> new NoSuchElementException("No such account"));
    }

    /** MDM-3: the full flat node list for a company's Chart of Accounts — clients build the tree from {@code parentId}. */
    @Transactional(readOnly = true)
    public List<Account> listForCompany(UUID companyId) {
        return accountRepository.findByCompanyId(companyId);
    }

    private Account requireGroupParent(UUID parentId) {
        if (parentId == null) {
            return null;
        }
        Account parent = accountRepository.findById(parentId).orElseThrow(() -> new NoSuchElementException("No such parent account"));
        if (!parent.isGroup()) {
            throw new IllegalArgumentException("Parent account must be a group account, not a ledger account");
        }
        return parent;
    }
}
