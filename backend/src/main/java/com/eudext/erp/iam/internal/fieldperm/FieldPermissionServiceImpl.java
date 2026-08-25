package com.eudext.erp.iam.internal.fieldperm;

import com.eudext.erp.iam.FieldAccess;
import com.eudext.erp.iam.FieldPermissionApi;
import com.eudext.erp.iam.internal.rbac.EffectivePermissions;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM-5: resolves effective field access as the most permissive value
 * across every role the user holds — matching {@link
 * com.eudext.erp.iam.internal.recordscope.RecordScopeServiceImpl}'s
 * "any role that permits it, permits it" combining rule. A role with no
 * explicit row for a field is unrestricted (WRITE) on it, per the V8
 * migration comment.
 */
@Service
public class FieldPermissionServiceImpl implements FieldPermissionApi {

    private final EffectivePermissions effectivePermissions;
    private final FieldPermissionRepository repository;

    public FieldPermissionServiceImpl(EffectivePermissions effectivePermissions, FieldPermissionRepository repository) {
        this.effectivePermissions = effectivePermissions;
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public FieldAccess resolveAccess(UUID userId, UUID companyId, String entityCode, String fieldName) {
        List<UUID> roleIds = effectivePermissions.roleIds(userId, companyId);
        if (roleIds.isEmpty()) {
            return FieldAccess.NONE;
        }

        List<FieldPermission> restrictions =
                repository.findByRoleIdInAndEntityCodeAndFieldName(roleIds, entityCode, fieldName);
        java.util.Set<UUID> restrictedRoleIds =
                restrictions.stream().map(FieldPermission::getRoleId).collect(java.util.stream.Collectors.toSet());

        FieldAccess best = FieldAccess.NONE;
        for (UUID roleId : roleIds) {
            FieldAccess roleAccess = restrictedRoleIds.contains(roleId)
                    ? restrictions.stream()
                            .filter(r -> r.getRoleId().equals(roleId))
                            .findFirst()
                            .map(FieldPermission::getAccess)
                            .orElse(FieldAccess.WRITE)
                    : FieldAccess.WRITE;
            if (roleAccess.ordinal() > best.ordinal()) {
                best = roleAccess;
            }
        }
        return best;
    }
}
