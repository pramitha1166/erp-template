package com.eudext.erp.iam.internal.rbac;

import com.eudext.erp.iam.AuthAuditEvents;
import com.eudext.erp.iam.PermissionCode;
import com.eudext.erp.iam.internal.sod.SegregationOfDutiesService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** IAM-3: role CRUD and permission grant/revoke. */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final SegregationOfDutiesService segregationOfDutiesService;
    private final ApplicationEventPublisher events;

    public RoleService(
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            SegregationOfDutiesService segregationOfDutiesService,
            ApplicationEventPublisher events) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.segregationOfDutiesService = segregationOfDutiesService;
        this.events = events;
    }

    @Transactional
    public Role createRole(UUID tenantId, String name, String description) {
        return roleRepository.save(Role.create(tenantId, name, description));
    }

    /**
     * Grants a permission to a role. SoD (IAM-7) is checked against every
     * user who already holds this role in any company — granting a new
     * permission to a role can just as easily create a conflict for an
     * existing holder as assigning a new role would.
     */
    @Transactional
    public void grantPermission(UUID tenantId, UUID roleId, String permissionCodeRaw, String grantedBy) {
        PermissionCode code = PermissionCode.parse(permissionCodeRaw);
        Set<String> existing = rolePermissionRepository.findByRoleId(roleId).stream()
                .map(RolePermission::getPermissionCode)
                .collect(Collectors.toSet());
        if (existing.contains(code.toString())) {
            return;
        }
        Set<String> afterGrant = new java.util.HashSet<>(existing);
        afterGrant.add(code.toString());
        segregationOfDutiesService.assertNoConflict(afterGrant);

        rolePermissionRepository.save(RolePermission.of(tenantId, roleId, code.toString()));
        events.publishEvent(new AuthAuditEvents.PermissionGranted(tenantId, roleId, code.toString(), grantedBy, Instant.now()));
    }

    @Transactional
    public void revokePermission(UUID tenantId, UUID roleId, String permissionCodeRaw, String revokedBy) {
        String code = PermissionCode.parse(permissionCodeRaw).toString();
        rolePermissionRepository.findByRoleIdAndPermissionCode(roleId, code).ifPresent(grant -> {
            rolePermissionRepository.delete(grant);
            events.publishEvent(new AuthAuditEvents.PermissionRevoked(tenantId, roleId, code, revokedBy, Instant.now()));
        });
    }

    @Transactional(readOnly = true)
    public List<RolePermission> permissionsOf(UUID roleId) {
        return rolePermissionRepository.findByRoleId(roleId);
    }

    /** IAM-2: whether any permission on this role has the `approve` action. */
    @Transactional(readOnly = true)
    public boolean grantsApprovalPermission(UUID roleId) {
        return rolePermissionRepository.findByRoleId(roleId).stream()
                .anyMatch(grant -> PermissionCode.parse(grant.getPermissionCode()).action().equals("approve"));
    }
}
