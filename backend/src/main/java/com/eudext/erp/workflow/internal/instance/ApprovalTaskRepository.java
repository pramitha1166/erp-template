package com.eudext.erp.workflow.internal.instance;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, UUID> {

    List<ApprovalTask> findByInstanceId(UUID instanceId);

    List<ApprovalTask> findByInstanceIdAndStatus(UUID instanceId, TaskStatus status);

    List<ApprovalTask> findByInstanceIdAndStepIdAndStatus(UUID instanceId, UUID stepId, TaskStatus status);

    long countByInstanceIdAndSequenceOrderAndStatus(UUID instanceId, int sequenceOrder, TaskStatus status);

    List<ApprovalTask> findByAssignedUserIdAndStatus(UUID assignedUserId, TaskStatus status);

    List<ApprovalTask> findByStatusAndDueAtBefore(TaskStatus status, Instant dueAt);
}
