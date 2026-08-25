package com.eudext.erp.iam.internal.rbac;

import com.eudext.erp.iam.AuthAuditEvents;
import com.eudext.erp.iam.internal.sod.SegregationOfDutiesService;
import com.eudext.erp.iam.internal.user.User;
import com.eudext.erp.iam.internal.user.UserRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** IAM-4: assigns/revokes a role for a user within a specific company. */
@Service
public class UserRoleAssignmentService {

    private final UserCompanyRoleRepository userCompanyRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleService roleService;
    private final UserRepository userRepository;
    private final SegregationOfDutiesService segregationOfDutiesService;
    private final ApplicationEventPublisher events;

    public UserRoleAssignmentService(
            UserCompanyRoleRepository userCompanyRoleRepository,
            RolePermissionRepository rolePermissionRepository,
            RoleService roleService,
            UserRepository userRepository,
            SegregationOfDutiesService segregationOfDutiesService,
            ApplicationEventPublisher events) {
        this.userCompanyRoleRepository = userCompanyRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleService = roleService;
        this.userRepository = userRepository;
        this.segregationOfDutiesService = segregationOfDutiesService;
        this.events = events;
    }

    @Transactional
    public void assign(UUID tenantId, UUID userId, UUID companyId, UUID roleId, String assignedBy) {
        if (userCompanyRoleRepository.findByUserIdAndCompanyIdAndRoleId(userId, companyId, roleId).isPresent()) {
            return;
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("No such user"));
        if (roleService.grantsApprovalPermission(roleId) && !user.isTotpEnabled()) {
            throw new TotpRequiredException();
        }

        Set<String> effectiveAfterAssignment = new HashSet<>(currentPermissionCodes(userId, companyId));
        effectiveAfterAssignment.addAll(permissionCodesOf(roleId));
        segregationOfDutiesService.assertNoConflict(effectiveAfterAssignment);

        userCompanyRoleRepository.save(UserCompanyRole.of(tenantId, userId, companyId, roleId));
        events.publishEvent(new AuthAuditEvents.RoleAssigned(tenantId, userId, companyId, roleId, assignedBy, Instant.now()));
    }

    @Transactional
    public void unassign(UUID tenantId, UUID userId, UUID companyId, UUID roleId, String unassignedBy) {
        userCompanyRoleRepository
                .findByUserIdAndCompanyIdAndRoleId(userId, companyId, roleId)
                .ifPresent(assignment -> {
                    userCompanyRoleRepository.delete(assignment);
                    events.publishEvent(
                            new AuthAuditEvents.RoleUnassigned(tenantId, userId, companyId, roleId, unassignedBy, Instant.now()));
                });
    }

    @Transactional(readOnly = true)
    public List<UserCompanyRole> rolesOf(UUID userId, UUID companyId) {
        return userCompanyRoleRepository.findByUserIdAndCompanyId(userId, companyId);
    }

    private Set<String> currentPermissionCodes(UUID userId, UUID companyId) {
        Set<String> codes = new HashSet<>();
        for (UserCompanyRole assignment : userCompanyRoleRepository.findByUserIdAndCompanyId(userId, companyId)) {
            codes.addAll(permissionCodesOf(assignment.getRoleId()));
        }
        return codes;
    }

    private Set<String> permissionCodesOf(UUID roleId) {
        Set<String> codes = new HashSet<>();
        for (RolePermission grant : rolePermissionRepository.findByRoleId(roleId)) {
            codes.add(grant.getPermissionCode());
        }
        return codes;
    }
}
