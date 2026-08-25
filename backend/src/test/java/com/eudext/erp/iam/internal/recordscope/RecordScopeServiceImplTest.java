package com.eudext.erp.iam.internal.recordscope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.eudext.erp.iam.RecordScopeType;
import com.eudext.erp.iam.internal.rbac.EffectivePermissions;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordScopeServiceImplTest {

    @Mock
    private EffectivePermissions effectivePermissions;

    @Mock
    private RecordScopeRestrictionRepository repository;

    private RecordScopeServiceImpl service;
    private final UUID userId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();
    private final UUID branchA = UUID.randomUUID();
    private final UUID branchB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RecordScopeServiceImpl(effectivePermissions, repository);
    }

    @Test
    void userWithNoRolesSeesNothing() {
        when(effectivePermissions.roleIds(userId, companyId)).thenReturn(List.of());

        assertThat(service.isRecordVisible(userId, companyId, RecordScopeType.BRANCH, branchA)).isFalse();
        assertThat(service.isUnrestricted(userId, companyId, RecordScopeType.BRANCH)).isFalse();
    }

    @Test
    void roleWithNoRestrictionsSeesEverything() {
        when(effectivePermissions.roleIds(userId, companyId)).thenReturn(List.of(roleId));
        when(repository.findByRoleIdInAndScopeType(List.of(roleId), RecordScopeType.BRANCH)).thenReturn(List.of());

        assertThat(service.isUnrestricted(userId, companyId, RecordScopeType.BRANCH)).isTrue();
        assertThat(service.isRecordVisible(userId, companyId, RecordScopeType.BRANCH, branchA)).isTrue();
        assertThat(service.isRecordVisible(userId, companyId, RecordScopeType.BRANCH, branchB)).isTrue();
    }

    @Test
    void restrictedRoleOnlySeesListedValues() {
        when(effectivePermissions.roleIds(userId, companyId)).thenReturn(List.of(roleId));
        RecordScopeRestriction restriction = RecordScopeRestriction.of(UUID.randomUUID(), roleId, RecordScopeType.BRANCH, branchA);
        when(repository.findByRoleIdInAndScopeType(List.of(roleId), RecordScopeType.BRANCH)).thenReturn(List.of(restriction));

        assertThat(service.isUnrestricted(userId, companyId, RecordScopeType.BRANCH)).isFalse();
        assertThat(service.isRecordVisible(userId, companyId, RecordScopeType.BRANCH, branchA)).isTrue();
        assertThat(service.isRecordVisible(userId, companyId, RecordScopeType.BRANCH, branchB)).isFalse();
        assertThat(service.allowedScopeValues(userId, companyId, RecordScopeType.BRANCH)).containsExactly(branchA);
    }

    @Test
    void anyUnrestrictedHeldRoleMakesTheUserUnrestrictedOverall() {
        UUID restrictedRole = UUID.randomUUID();
        UUID unrestrictedRole = UUID.randomUUID();
        when(effectivePermissions.roleIds(userId, companyId)).thenReturn(List.of(restrictedRole, unrestrictedRole));
        RecordScopeRestriction restriction =
                RecordScopeRestriction.of(UUID.randomUUID(), restrictedRole, RecordScopeType.BRANCH, branchA);
        when(repository.findByRoleIdInAndScopeType(List.of(restrictedRole, unrestrictedRole), RecordScopeType.BRANCH))
                .thenReturn(List.of(restriction));

        assertThat(service.isUnrestricted(userId, companyId, RecordScopeType.BRANCH)).isTrue();
        assertThat(service.isRecordVisible(userId, companyId, RecordScopeType.BRANCH, branchB)).isTrue();
    }
}
