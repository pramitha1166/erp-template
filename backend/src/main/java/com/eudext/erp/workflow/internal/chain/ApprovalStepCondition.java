package com.eudext.erp.workflow.internal.chain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * WF-2: one field condition on a step. A step applies to an instance only
 * if every one of its conditions matches (AND semantics; no conditions
 * means the step always applies). Exactly one of {@code valueString} /
 * {@code valueNumber} is set, matching whether the compared document field
 * is textual or numeric; numeric thresholds are {@link BigDecimal}, never
 * {@code double}/{@code float} (ARCH-5).
 */
@Entity
@Table(name = "approval_step_conditions")
public class ApprovalStepCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "step_id", nullable = false, updatable = false)
    private UUID stepId;

    @Column(name = "field_name", nullable = false, updatable = false)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, updatable = false)
    private ConditionOperator operator;

    @Column(name = "value_string", updatable = false)
    private String valueString;

    @Column(name = "value_number", updatable = false)
    private BigDecimal valueNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ApprovalStepCondition() {}

    public static ApprovalStepCondition ofNumber(
            UUID tenantId, UUID stepId, String fieldName, ConditionOperator operator, BigDecimal value) {
        ApprovalStepCondition condition = base(tenantId, stepId, fieldName, operator);
        condition.valueNumber = value;
        return condition;
    }

    public static ApprovalStepCondition ofText(
            UUID tenantId, UUID stepId, String fieldName, ConditionOperator operator, String value) {
        ApprovalStepCondition condition = base(tenantId, stepId, fieldName, operator);
        condition.valueString = value;
        return condition;
    }

    private static ApprovalStepCondition base(UUID tenantId, UUID stepId, String fieldName, ConditionOperator operator) {
        ApprovalStepCondition condition = new ApprovalStepCondition();
        condition.tenantId = tenantId;
        condition.stepId = stepId;
        condition.fieldName = fieldName;
        condition.operator = operator;
        condition.createdAt = Instant.now();
        return condition;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStepId() {
        return stepId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public ConditionOperator getOperator() {
        return operator;
    }

    public String getValueString() {
        return valueString;
    }

    public BigDecimal getValueNumber() {
        return valueNumber;
    }
}
