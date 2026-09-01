package com.eudext.erp.workflow.internal.chain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, UUID> {

    List<ApprovalStep> findByChainIdOrderBySequenceOrderAsc(UUID chainId);
}
