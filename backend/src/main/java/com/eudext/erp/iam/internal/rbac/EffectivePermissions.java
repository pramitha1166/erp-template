package com.eudext.erp.iam.internal.rbac;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared resolution of "which roles does this user hold in this company,
 * and what permissions do those roles grant" — used by {@link
 * PermissionServiceImpl}, {@code FieldPermissionServiceImpl}, and {@code
 * RecordScopeServiceImpl} alike so the notion of "effective roles" stays
 * single-sourced across every IAM-5/6/7 engine.
 */
@Component
public class EffectivePermissions {

    private final UserCompanyRoleRepository userCompanyRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public EffectivePermissions(
            UserCompanyRoleRepository userCompanyRoleRepository, RolePermissionRepository rolePermissionRepository) {
        this.userCompanyRoleRepository = userCompanyRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Transactional(readOnly = true)
    public List<UUID> roleIds(UUID userId, UUID companyId) {
        return userCompanyRoleRepository.findByUserIdAndCompanyId(userId, companyId).stream()
                .map(UserCompanyRole::getRoleId)
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<String> permissionCodes(UUID userId, UUID companyId) {
        List<UUID> roleIds = roleIds(userId, companyId);
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return rolePermissionRepository.findByRoleIdIn(roleIds).stream()
                .map(RolePermission::getPermissionCode)
                .collect(Collectors.toSet());
    }
}
