package com.eudext.erp.workflow.internal.instance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowInstanceStepRepository extends JpaRepository<WorkflowInstanceStep, UUID> {

    List<WorkflowInstanceStep> findByInstanceIdOrderBySequenceOrderAsc(UUID instanceId);

    List<WorkflowInstanceStep> findByInstanceIdAndSequenceOrder(UUID instanceId, int sequenceOrder);
}
