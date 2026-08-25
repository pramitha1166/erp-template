package com.eudext.erp.iam.internal.rbac;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCompanyRoleRepository extends JpaRepository<UserCompanyRole, UUID> {

    List<UserCompanyRole> findByUserIdAndCompanyId(UUID userId, UUID companyId);

    List<UserCompanyRole> findByUserId(UUID userId);

    Optional<UserCompanyRole> findByUserIdAndCompanyIdAndRoleId(UUID userId, UUID companyId, UUID roleId);
}
