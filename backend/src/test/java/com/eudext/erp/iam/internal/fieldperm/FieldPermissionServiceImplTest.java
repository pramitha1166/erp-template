package com.eudext.erp.iam.internal.fieldperm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.eudext.erp.iam.FieldAccess;
import com.eudext.erp.iam.internal.rbac.EffectivePermissions;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Example entity is `payroll:employee.salary` — the SRS's own field-gating example (IAM-5). */
@ExtendWith(MockitoExtension.class)
class FieldPermissionServiceImplTest {

    @Mock
    private EffectivePermissions effectivePermissions;

    @Mock
    private FieldPermissionRepository repository;

    private FieldPermissionServiceImpl service;
    private final UUID userId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FieldPermissionServiceImpl(effectivePermissions, repository);
    }

    @Test
    void userWithNoRolesHasNoAccess() {
        when(effectivePermissions.roleIds(userId, companyId)).thenReturn(List.of());

        assertThat(service.resolveAccess(userId, companyId, "payroll:employee", "salary")).isEqualTo(FieldAccess.NONE);
    }

    @Test
    void roleWithNoExplicitRestrictionDefaultsToWrite() {
        when(effectivePermissions.roleIds(userId, companyId)).thenReturn(List.of(roleId));
        when(repository.findByRoleIdInAndEntityCodeAndFieldName(List.of(roleId), "payroll:employee", "salary"))
                .thenReturn(List.of());

        assertThat(service.resolveAccess(userId, companyId, "payroll:employee", "salary")).isEqualTo(FieldAccess.WRITE);
    }

    @Test
    void explicitRestrictionIsHonored() {
        when(effectivePermissions.roleIds(userId, companyId)).thenReturn(List.of(roleId));
        FieldPermission restriction = FieldPermission.of(UUID.randomUUID(), roleId, "payroll:employee", "salary", FieldAccess.NONE);
        when(repository.findByRoleIdInAndEntityCodeAndFieldName(List.of(roleId), "payroll:employee", "salary"))
                .thenReturn(List.of(restriction));

        assertThat(service.resolveAccess(userId, companyId, "payroll:employee", "salary")).isEqualTo(FieldAccess.NONE);
    }

    @Test
    void mostPermissiveRoleWinsWhenMultipleRolesAreHeld() {
        UUID restrictedRole = UUID.randomUUID();
        UUID unrestrictedRole = UUID.randomUUID();
        when(effectivePermissions.roleIds(userId, companyId)).thenReturn(List.of(restrictedRole, unrestrictedRole));
        FieldPermission restriction =
                FieldPermission.of(UUID.randomUUID(), restrictedRole, "payroll:employee", "salary", FieldAccess.NONE);
        when(repository.findByRoleIdInAndEntityCodeAndFieldName(
                        List.of(restrictedRole, unrestrictedRole), "payroll:employee", "salary"))
                .thenReturn(List.of(restriction));

        // unrestrictedRole has no row => WRITE, which beats restrictedRole's explicit NONE.
        assertThat(service.resolveAccess(userId, companyId, "payroll:employee", "salary")).isEqualTo(FieldAccess.WRITE);
    }
}
