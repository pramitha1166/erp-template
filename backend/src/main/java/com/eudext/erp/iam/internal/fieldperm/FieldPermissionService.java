package com.eudext.erp.iam.internal.fieldperm;

import com.eudext.erp.iam.FieldAccess;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** IAM-5: admin-facing CRUD for a role's field-level permissions. */
@Service
public class FieldPermissionService {

    private final FieldPermissionRepository repository;

    public FieldPermissionService(FieldPermissionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void setAccess(UUID tenantId, UUID roleId, String entityCode, String fieldName, FieldAccess access) {
        FieldPermission permission = repository
                .findByRoleIdAndEntityCodeAndFieldName(roleId, entityCode, fieldName)
                .orElseGet(() -> FieldPermission.of(tenantId, roleId, entityCode, fieldName, access));
        permission.setAccess(access);
        repository.save(permission);
    }

    @Transactional(readOnly = true)
    public List<FieldPermission> listForRole(UUID roleId) {
        return repository.findByRoleId(roleId);
    }
}
