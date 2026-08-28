package com.eudext.erp.iam.internal.provisioning;

import com.eudext.erp.iam.IdentityProvisioningApi;
import com.eudext.erp.iam.internal.rbac.RolePermissionRepository;
import com.eudext.erp.iam.internal.rbac.RoleService;
import com.eudext.erp.iam.internal.rbac.UserCompanyRoleRepository;
import com.eudext.erp.iam.internal.rbac.UserRoleAssignmentService;
import com.eudext.erp.iam.internal.user.User;
import com.eudext.erp.iam.internal.user.UserRepository;
import com.eudext.erp.iam.internal.user.UserService;
import com.eudext.erp.iam.internal.user.UserStatus;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backs {@link IdentityProvisioningApi}. Every method here assumes the
 * caller has already set {@code TenantContext} (directly, or via {@code
 * TenantContextScope}) to the {@code tenantId} it passes in — the same
 * precondition every other RLS-scoped repository call in this codebase
 * relies on.
 */
@Service
class IdentityProvisioningServiceImpl implements IdentityProvisioningApi {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final UserRoleAssignmentService userRoleAssignmentService;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserCompanyRoleRepository userCompanyRoleRepository;

    IdentityProvisioningServiceImpl(
            UserService userService,
            UserRepository userRepository,
            RoleService roleService,
            UserRoleAssignmentService userRoleAssignmentService,
            RolePermissionRepository rolePermissionRepository,
            UserCompanyRoleRepository userCompanyRoleRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.userRoleAssignmentService = userRoleAssignmentService;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userCompanyRoleRepository = userCompanyRoleRepository;
    }

    @Override
    @Transactional
    public ProvisionedUser provisionTenantUser(UUID tenantId, String email) {
        return provisionTenantUser(tenantId, email, TemporaryPasswordGenerator.generate());
    }

    @Override
    @Transactional
    public ProvisionedUser provisionTenantUser(UUID tenantId, String email, String chosenPassword) {
        User user = userService.createUser(tenantId, email, chosenPassword);
        return new ProvisionedUser(user.getId(), user.getEmail(), chosenPassword);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailInUse(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public String emailOf(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("No such user")).getEmail();
    }

    @Override
    @Transactional
    public UUID createRole(UUID tenantId, String name, String description) {
        return roleService.createRole(tenantId, name, description).getId();
    }

    @Override
    @Transactional
    public void grantPermission(UUID tenantId, UUID roleId, String permissionCode) {
        roleService.grantPermission(tenantId, roleId, permissionCode, "system:onboarding");
    }

    @Override
    @Transactional
    public void assignRole(UUID tenantId, UUID userId, UUID companyId, UUID roleId, String assignedBy) {
        userRoleAssignmentService.assign(tenantId, userId, companyId, roleId, assignedBy);
    }

    @Override
    @Transactional
    public void setUserActive(UUID tenantId, UUID userId, boolean active) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("No such user"));
        if (!user.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("User does not belong to tenant " + tenantId);
        }
        if (active) {
            user.activate();
        } else {
            user.disable();
        }
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveUsers() {
        return userRepository.countByStatus(UserStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean anyUserHoldsPermission(UUID companyId, String permissionCode) {
        List<UUID> roleIds = rolePermissionRepository.findByPermissionCode(permissionCode).stream()
                .map(grant -> grant.getRoleId())
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return false;
        }
        return userCompanyRoleRepository.existsByCompanyIdAndRoleIdIn(companyId, roleIds);
    }
}
