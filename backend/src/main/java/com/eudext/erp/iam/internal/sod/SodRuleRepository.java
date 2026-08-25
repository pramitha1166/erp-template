package com.eudext.erp.iam.internal.sod;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SodRuleRepository extends JpaRepository<SodRule, UUID> {

    List<SodRule> findByActiveTrue();
}
