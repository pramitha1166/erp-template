package com.eudext.erp.iam.internal.rbac;

import com.eudext.erp.iam.PermissionApi;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionServiceImpl implements PermissionApi {

    private final EffectivePermissions effectivePermissions;

    public PermissionServiceImpl(EffectivePermissions effectivePermissions) {
        this.effectivePermissions = effectivePermissions;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(UUID userId, UUID companyId, String permissionCode) {
        return effectivePermissions.permissionCodes(userId, companyId).contains(permissionCode);
    }
}
