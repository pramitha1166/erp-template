package com.eudext.erp.iam.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.iam.FieldAccess;
import com.eudext.erp.iam.RecordScopeType;
import com.eudext.erp.iam.internal.auth.AccessControlService;
import com.eudext.erp.iam.internal.fieldperm.FieldPermissionService;
import com.eudext.erp.iam.internal.rbac.Role;
import com.eudext.erp.iam.internal.rbac.RoleService;
import com.eudext.erp.iam.internal.recordscope.RecordScopeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** IAM-3 / IAM-5 / IAM-6: role CRUD, permission grants, and the field/record-level restrictions layered on a role. */
@RestController
@RequestMapping("/iam/roles")
public class RoleController {

    private static final String PERMISSION_CREATE_ROLE = "iam:role:create";
    private static final String PERMISSION_MANAGE_ROLE = "iam:role:manage";

    private final RoleService roleService;
    private final FieldPermissionService fieldPermissionService;
    private final RecordScopeService recordScopeService;
    private final AccessControlService accessControlService;

    public RoleController(
            RoleService roleService,
            FieldPermissionService fieldPermissionService,
            RecordScopeService recordScopeService,
            AccessControlService accessControlService) {
        this.roleService = roleService;
        this.fieldPermissionService = fieldPermissionService;
        this.recordScopeService = recordScopeService;
        this.accessControlService = accessControlService;
    }

    public record CreateRoleRequest(@NotBlank String name, String description) {}

    public record RoleView(UUID id, String name, String description) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleView createRole(@RequestParam UUID companyId, @Valid @RequestBody CreateRoleRequest request) {
        accessControlService.requirePermission(companyId, PERMISSION_CREATE_ROLE);
        Role role = roleService.createRole(tenantId(), request.name(), request.description());
        return new RoleView(role.getId(), role.getName(), role.getDescription());
    }

    public record GrantPermissionRequest(@NotBlank String permissionCode) {}

    @PostMapping("/{roleId}/permissions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grantPermission(
            @PathVariable UUID roleId, @RequestParam UUID companyId, @Valid @RequestBody GrantPermissionRequest request) {
        UUID grantedBy = accessControlService.requirePermission(companyId, PERMISSION_MANAGE_ROLE);
        roleService.grantPermission(tenantId(), roleId, request.permissionCode(), grantedBy.toString());
    }

    @DeleteMapping("/{roleId}/permissions/{permissionCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePermission(
            @PathVariable UUID roleId, @PathVariable String permissionCode, @RequestParam UUID companyId) {
        UUID revokedBy = accessControlService.requirePermission(companyId, PERMISSION_MANAGE_ROLE);
        roleService.revokePermission(tenantId(), roleId, permissionCode, revokedBy.toString());
    }

    public record PermissionView(String permissionCode) {}

    @GetMapping("/{roleId}/permissions")
    public List<PermissionView> listPermissions(@PathVariable UUID roleId) {
        return roleService.permissionsOf(roleId).stream()
                .map(p -> new PermissionView(p.getPermissionCode()))
                .toList();
    }

    public record FieldPermissionRequest(@NotBlank String entityCode, @NotBlank String fieldName, @NotNull FieldAccess access) {}

    @PostMapping("/{roleId}/field-permissions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setFieldPermission(
            @PathVariable UUID roleId, @RequestParam UUID companyId, @Valid @RequestBody FieldPermissionRequest request) {
        accessControlService.requirePermission(companyId, PERMISSION_MANAGE_ROLE);
        fieldPermissionService.setAccess(tenantId(), roleId, request.entityCode(), request.fieldName(), request.access());
    }

    public record RecordScopeRequest(@NotNull RecordScopeType scopeType, @NotNull UUID scopeValue) {}

    @PostMapping("/{roleId}/record-scopes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addRecordScope(
            @PathVariable UUID roleId, @RequestParam UUID companyId, @Valid @RequestBody RecordScopeRequest request) {
        accessControlService.requirePermission(companyId, PERMISSION_MANAGE_ROLE);
        recordScopeService.addRestriction(tenantId(), roleId, request.scopeType(), request.scopeValue());
    }

    @DeleteMapping("/{roleId}/record-scopes/{restrictionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRecordScope(
            @PathVariable UUID roleId, @PathVariable UUID restrictionId, @RequestParam UUID companyId) {
        accessControlService.requirePermission(companyId, PERMISSION_MANAGE_ROLE);
        recordScopeService.removeRestriction(restrictionId);
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
