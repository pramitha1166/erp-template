package com.eudext.erp.masterdata.internal.currency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, UUID> {

    List<Currency> findByTenantId(UUID tenantId);

    Optional<Currency> findByTenantIdAndCode(UUID tenantId, String code);
}
