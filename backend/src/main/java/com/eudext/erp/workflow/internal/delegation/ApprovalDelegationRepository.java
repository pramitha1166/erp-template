package com.eudext.erp.workflow.internal.delegation;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalDelegationRepository extends JpaRepository<ApprovalDelegation, UUID> {

    List<ApprovalDelegation> findByDelegatorUserId(UUID delegatorUserId);

    List<ApprovalDelegation> findByDelegatorUserIdAndRevokedFalse(UUID delegatorUserId);
}
