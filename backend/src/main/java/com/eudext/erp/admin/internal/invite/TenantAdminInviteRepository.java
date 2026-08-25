package com.eudext.erp.admin.internal.invite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantAdminInviteRepository extends JpaRepository<TenantAdminInvite, UUID> {

    List<TenantAdminInvite> findByTenantId(UUID tenantId);

    Optional<TenantAdminInvite> findByTokenHash(String tokenHash);
}
