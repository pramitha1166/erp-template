package com.eudext.erp.masterdata.internal.coa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** MDM-3: the group-vs-ledger tree invariant beyond the onboarding seed. */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository repository;

    private AccountService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AccountService(repository);
    }

    @Test
    void topLevelAccountNeedsNoParent() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account account = service.create(tenantId, companyId, "1000", "Assets", AccountType.ASSET, null, true);
        assertThat(account.getParentId()).isNull();
    }

    @Test
    void childAccountUnderAGroupParentIsAccepted() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Account parent = Account.create(tenantId, companyId, "1000", "Assets", AccountType.ASSET, null, true);
        UUID parentId = UUID.randomUUID();
        setId(parent, parentId);
        when(repository.findById(parentId)).thenReturn(Optional.of(parent));

        Account child = service.create(tenantId, companyId, "1100", "Current Assets", AccountType.ASSET, parentId, true);
        assertThat(child.getParentId()).isEqualTo(parentId);
    }

    @Test
    void rejectsALedgerAccountAsAParent() {
        Account ledgerParent = Account.create(tenantId, companyId, "1110", "Cash", AccountType.ASSET, null, false);
        UUID parentId = UUID.randomUUID();
        setId(ledgerParent, parentId);
        when(repository.findById(parentId)).thenReturn(Optional.of(ledgerParent));

        assertThatThrownBy(() -> service.create(tenantId, companyId, "1111", "Petty Cash", AccountType.ASSET, parentId, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("group account");
    }

    @Test
    void rejectsAChildWhoseTypeDiffersFromItsParent() {
        Account parent = Account.create(tenantId, companyId, "1000", "Assets", AccountType.ASSET, null, true);
        UUID parentId = UUID.randomUUID();
        setId(parent, parentId);
        when(repository.findById(parentId)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> service.create(tenantId, companyId, "2000", "Liabilities", AccountType.LIABILITY, parentId, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deactivateAndActivateToggleTheActiveFlag() {
        Account account = Account.create(tenantId, companyId, "1000", "Assets", AccountType.ASSET, null, true);
        UUID id = UUID.randomUUID();
        setId(account, id);
        when(repository.findById(id)).thenReturn(Optional.of(account));

        service.deactivate(id);
        assertThat(account.isActive()).isFalse();

        service.activate(id);
        assertThat(account.isActive()).isTrue();
    }

    private static void setId(Account account, UUID id) {
        try {
            var idField = Account.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(account, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
