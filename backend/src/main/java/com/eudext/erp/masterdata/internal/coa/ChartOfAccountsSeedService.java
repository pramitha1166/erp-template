package com.eudext.erp.masterdata.internal.coa;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ADM-3: seeds {@link ChartOfAccountsSeedTemplate} into a company's Chart of Accounts. */
@Service
public class ChartOfAccountsSeedService {

    private final AccountRepository accountRepository;

    public ChartOfAccountsSeedService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /** No-op if the company already has accounts — seeding is meant to run exactly once, at onboarding. */
    @Transactional
    public void seedDefault(UUID tenantId, UUID companyId, boolean includeSriLankaStatutoryAccounts) {
        if (accountRepository.existsByCompanyId(companyId)) {
            return;
        }
        Map<String, UUID> codeToId = new HashMap<>();
        for (ChartOfAccountsSeedTemplate.Entry entry : ChartOfAccountsSeedTemplate.standard()) {
            if (entry.lkStatutory() && !includeSriLankaStatutoryAccounts) {
                continue;
            }
            UUID parentId = entry.parentCode() == null ? null : codeToId.get(entry.parentCode());
            Account account = accountRepository.save(
                    Account.create(tenantId, companyId, entry.code(), entry.name(), entry.type(), parentId, entry.group()));
            codeToId.put(entry.code(), account.getId());
        }
    }
}
