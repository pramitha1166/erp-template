package com.eudext.erp.masterdata.internal.coa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ADM-3: onboarding's default Chart of Accounts seed. */
@ExtendWith(MockitoExtension.class)
class ChartOfAccountsSeedServiceTest {

    @Mock
    private AccountRepository repository;

    private ChartOfAccountsSeedService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ChartOfAccountsSeedService(repository);
    }

    @Test
    void skipsSriLankaStatutoryAccountsWhenNotEntitled() {
        when(repository.existsByCompanyId(companyId)).thenReturn(false);
        when(repository.save(ArgumentMatchers.any())).thenAnswer(inv -> withGeneratedId(inv.getArgument(0)));

        service.seedDefault(tenantId, companyId, false);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Account::getCode).doesNotContain("2120", "2130", "2140", "2150", "5310", "5320");
    }

    @Test
    void includesSriLankaStatutoryAccountsWhenEntitled() {
        when(repository.existsByCompanyId(companyId)).thenReturn(false);
        when(repository.save(ArgumentMatchers.any())).thenAnswer(inv -> withGeneratedId(inv.getArgument(0)));

        service.seedDefault(tenantId, companyId, true);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Account::getCode).contains("2120", "2130", "2140", "2150", "5310", "5320");
    }

    @Test
    void childAccountsAreLinkedToTheirParentsRealGeneratedId() {
        when(repository.existsByCompanyId(companyId)).thenReturn(false);
        when(repository.save(ArgumentMatchers.any())).thenAnswer(inv -> withGeneratedId(inv.getArgument(0)));

        service.seedDefault(tenantId, companyId, true);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        var byCode = captor.getAllValues().stream().collect(java.util.stream.Collectors.toMap(Account::getCode, a -> a));

        Account cashAndBank = byCode.get("1110");
        Account currentAssets = byCode.get("1100");
        Account assets = byCode.get("1000");
        assertThat(cashAndBank.getParentId()).isEqualTo(currentAssets.getId());
        assertThat(currentAssets.getParentId()).isEqualTo(assets.getId());
        assertThat(assets.getParentId()).isNull(); // top-level group, no parent
    }

    @Test
    void isANoOpIfTheCompanyAlreadyHasAccounts() {
        when(repository.existsByCompanyId(companyId)).thenReturn(true);

        service.seedDefault(tenantId, companyId, true);

        verify(repository, never()).save(ArgumentMatchers.any());
    }

    private static Account withGeneratedId(Account account) {
        try {
            var idField = Account.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(account, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return account;
    }
}
