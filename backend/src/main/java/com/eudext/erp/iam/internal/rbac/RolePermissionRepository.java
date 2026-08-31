package com.eudext.erp.iam.internal.rbac;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    List<RolePermission> findByRoleId(UUID roleId);

    List<RolePermission> findByRoleIdIn(List<UUID> roleIds);

    Optional<RolePermission> findByRoleIdAndPermissionCode(UUID roleId, String permissionCode);

    List<RolePermission> findByPermissionCode(String permissionCode);
}
