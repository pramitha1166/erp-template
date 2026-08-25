package com.eudext.erp.iam.internal.recordscope;

import com.eudext.erp.iam.RecordScopeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordScopeRestrictionRepository extends JpaRepository<RecordScopeRestriction, UUID> {

    List<RecordScopeRestriction> findByRoleIdInAndScopeType(List<UUID> roleIds, RecordScopeType scopeType);

    List<RecordScopeRestriction> findByRoleId(UUID roleId);
}
