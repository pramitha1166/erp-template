package com.eudext.erp.iam.internal.fieldperm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldPermissionRepository extends JpaRepository<FieldPermission, UUID> {

    List<FieldPermission> findByRoleIdInAndEntityCodeAndFieldName(List<UUID> roleIds, String entityCode, String fieldName);

    List<FieldPermission> findByRoleId(UUID roleId);

    Optional<FieldPermission> findByRoleIdAndEntityCodeAndFieldName(UUID roleId, String entityCode, String fieldName);
}
