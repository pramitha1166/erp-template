package com.eudext.erp.workflow.internal.instance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A snapshot, taken once when an instance starts, of exactly which chain
 * steps applied (WF-2 conditions already evaluated against the field
 * values supplied at that moment). Group-advancement logic in
 * {@code WorkflowEngine} reads this rather than re-evaluating conditions
 * later, when the original field values are no longer available.
 */
@Entity
@Table(name = "workflow_instance_steps")
public class WorkflowInstanceStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "instance_id", nullable = false, updatable = false)
    private UUID instanceId;

    @Column(name = "step_id", nullable = false, updatable = false)
    private UUID stepId;

    @Column(name = "sequence_order", nullable = false, updatable = false)
    private int sequenceOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WorkflowInstanceStep() {}

    public static WorkflowInstanceStep of(UUID tenantId, UUID instanceId, UUID stepId, int sequenceOrder) {
        WorkflowInstanceStep planStep = new WorkflowInstanceStep();
        planStep.tenantId = tenantId;
        planStep.instanceId = instanceId;
        planStep.stepId = stepId;
        planStep.sequenceOrder = sequenceOrder;
        planStep.createdAt = Instant.now();
        return planStep;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public UUID getStepId() {
        return stepId;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }
}
