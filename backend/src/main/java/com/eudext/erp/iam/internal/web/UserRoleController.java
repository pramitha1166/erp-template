package com.eudext.erp.iam.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.iam.internal.auth.AccessControlService;
import com.eudext.erp.iam.internal.rbac.UserCompanyRole;
import com.eudext.erp.iam.internal.rbac.UserRoleAssignmentService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** IAM-4: assigning/revoking a role for a user within a specific company. */
@RestController
@RequestMapping("/iam/users/{userId}/roles")
public class UserRoleController {

    private static final String PERMISSION_ASSIGN_ROLE = "iam:user-role:assign";

    private final UserRoleAssignmentService assignmentService;
    private final AccessControlService accessControlService;

    public UserRoleController(UserRoleAssignmentService assignmentService, AccessControlService accessControlService) {
        this.assignmentService = assignmentService;
        this.accessControlService = accessControlService;
    }

    @PostMapping("/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assign(@PathVariable UUID userId, @PathVariable UUID roleId, @RequestParam UUID companyId) {
        UUID assignedBy = accessControlService.requirePermission(companyId, PERMISSION_ASSIGN_ROLE);
        assignmentService.assign(tenantId(), userId, companyId, roleId, assignedBy.toString());
    }

    @DeleteMapping("/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable UUID userId, @PathVariable UUID roleId, @RequestParam UUID companyId) {
        UUID unassignedBy = accessControlService.requirePermission(companyId, PERMISSION_ASSIGN_ROLE);
        assignmentService.unassign(tenantId(), userId, companyId, roleId, unassignedBy.toString());
    }

    public record RoleAssignmentView(UUID roleId) {}

    @GetMapping
    public List<RoleAssignmentView> list(@PathVariable UUID userId, @RequestParam UUID companyId) {
        return assignmentService.rolesOf(userId, companyId).stream()
                .map(a -> new RoleAssignmentView(a.getRoleId()))
                .toList();
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
