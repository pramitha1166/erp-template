package com.eudext.erp.masterdata.internal.coa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByCompanyId(UUID companyId);

    boolean existsByCompanyId(UUID companyId);
}
