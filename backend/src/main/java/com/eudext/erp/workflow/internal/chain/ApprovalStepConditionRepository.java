package com.eudext.erp.workflow.internal.chain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalStepConditionRepository extends JpaRepository<ApprovalStepCondition, UUID> {

    List<ApprovalStepCondition> findByStepId(UUID stepId);
}
