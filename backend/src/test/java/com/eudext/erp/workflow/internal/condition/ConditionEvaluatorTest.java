package com.eudext.erp.workflow.internal.condition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eudext.erp.workflow.internal.chain.ApprovalStepCondition;
import com.eudext.erp.workflow.internal.chain.ConditionOperator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** WF-2: condition matching against document field values. */
class ConditionEvaluatorTest {

    private final ConditionEvaluator evaluator = new ConditionEvaluator();
    private final UUID stepId = UUID.randomUUID();

    @Test
    void noConditionsAlwaysMatches() {
        assertThat(evaluator.matches(List.of(), Map.of())).isTrue();
    }

    @Test
    void numericGreaterThanAboveThresholdMatches() {
        var condition = ApprovalStepCondition.ofNumber(
                UUID.randomUUID(), stepId, "amount", ConditionOperator.GT, new BigDecimal("500000"));

        assertThat(evaluator.matches(List.of(condition), Map.of("amount", new BigDecimal("600000")))).isTrue();
        assertThat(evaluator.matches(List.of(condition), Map.of("amount", new BigDecimal("500000")))).isFalse();
        assertThat(evaluator.matches(List.of(condition), Map.of("amount", new BigDecimal("1")))).isFalse();
    }

    @Test
    void numericConditionAcceptsPlainIntegerInput() {
        var condition = ApprovalStepCondition.ofNumber(UUID.randomUUID(), stepId, "quantity", ConditionOperator.GTE, new BigDecimal("10"));

        assertThat(evaluator.matches(List.of(condition), Map.of("quantity", 10))).isTrue();
        assertThat(evaluator.matches(List.of(condition), Map.of("quantity", 9))).isFalse();
    }

    @Test
    void missingFieldNeverMatches() {
        var condition = ApprovalStepCondition.ofNumber(UUID.randomUUID(), stepId, "amount", ConditionOperator.GT, BigDecimal.ZERO);

        assertThat(evaluator.matches(List.of(condition), Map.of())).isFalse();
    }

    @Test
    void textEqualityMatches() {
        var condition = ApprovalStepCondition.ofText(UUID.randomUUID(), stepId, "currency", ConditionOperator.EQ, "LKR");

        assertThat(evaluator.matches(List.of(condition), Map.of("currency", "LKR"))).isTrue();
        assertThat(evaluator.matches(List.of(condition), Map.of("currency", "USD"))).isFalse();
    }

    @Test
    void allConditionsMustMatchForStepToApply() {
        var amountCondition =
                ApprovalStepCondition.ofNumber(UUID.randomUUID(), stepId, "amount", ConditionOperator.GT, new BigDecimal("500000"));
        var currencyCondition = ApprovalStepCondition.ofText(UUID.randomUUID(), stepId, "currency", ConditionOperator.EQ, "LKR");
        List<ApprovalStepCondition> conditions = List.of(amountCondition, currencyCondition);

        assertThat(evaluator.matches(conditions, Map.of("amount", new BigDecimal("600000"), "currency", "LKR")))
                .isTrue();
        assertThat(evaluator.matches(conditions, Map.of("amount", new BigDecimal("600000"), "currency", "USD")))
                .isFalse();
    }
}
