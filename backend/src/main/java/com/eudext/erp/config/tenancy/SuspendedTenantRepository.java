package com.eudext.erp.config.tenancy;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SuspendedTenantRepository extends JpaRepository<SuspendedTenantMarker, UUID> {}
