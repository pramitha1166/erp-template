package com.eudext.erp.iam.internal.recordscope;

import com.eudext.erp.iam.RecordScopeApi;
import com.eudext.erp.iam.RecordScopeType;
import com.eudext.erp.iam.internal.rbac.EffectivePermissions;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM-6: a role with zero restriction rows for a scope type is
 * unrestricted on it (sees everything); the user is permitted overall if
 * *any* held role permits it — see the V8 migration comment and the
 * {@link RecordScopeApi} javadoc for the full semantics.
 */
@Service
public class RecordScopeServiceImpl implements RecordScopeApi {

    private final EffectivePermissions effectivePermissions;
    private final RecordScopeRestrictionRepository repository;

    public RecordScopeServiceImpl(EffectivePermissions effectivePermissions, RecordScopeRestrictionRepository repository) {
        this.effectivePermissions = effectivePermissions;
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRecordVisible(UUID userId, UUID companyId, RecordScopeType scopeType, UUID scopeValue) {
        List<UUID> roleIds = effectivePermissions.roleIds(userId, companyId);
        if (roleIds.isEmpty()) {
            return false;
        }
        Set<UUID> restrictedRoleIds = new HashSet<>();
        Set<UUID> permittedValues = new HashSet<>();
        for (RecordScopeRestriction restriction : repository.findByRoleIdInAndScopeType(roleIds, scopeType)) {
            restrictedRoleIds.add(restriction.getRoleId());
            permittedValues.add(restriction.getScopeValue());
        }
        boolean anyUnrestrictedRole = roleIds.stream().anyMatch(id -> !restrictedRoleIds.contains(id));
        return anyUnrestrictedRole || permittedValues.contains(scopeValue);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUnrestricted(UUID userId, UUID companyId, RecordScopeType scopeType) {
        List<UUID> roleIds = effectivePermissions.roleIds(userId, companyId);
        if (roleIds.isEmpty()) {
            return false;
        }
        Set<UUID> restrictedRoleIds = new HashSet<>();
        for (RecordScopeRestriction restriction : repository.findByRoleIdInAndScopeType(roleIds, scopeType)) {
            restrictedRoleIds.add(restriction.getRoleId());
        }
        return roleIds.stream().anyMatch(id -> !restrictedRoleIds.contains(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> allowedScopeValues(UUID userId, UUID companyId, RecordScopeType scopeType) {
        List<UUID> roleIds = effectivePermissions.roleIds(userId, companyId);
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        Set<UUID> values = new HashSet<>();
        for (RecordScopeRestriction restriction : repository.findByRoleIdInAndScopeType(roleIds, scopeType)) {
            values.add(restriction.getScopeValue());
        }
        return values;
    }
}
