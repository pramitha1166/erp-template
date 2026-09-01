package com.eudext.erp.workflow.internal.condition;

import com.eudext.erp.workflow.internal.chain.ApprovalStepCondition;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * WF-2: evaluates a step's conditions against the field values supplied
 * when approval starts. A step with no conditions always matches; a step
 * with one or more conditions matches only if every one of them does (AND
 * semantics). A field the caller didn't supply never matches any
 * condition on it.
 */
@Component
public class ConditionEvaluator {

    public boolean matches(List<ApprovalStepCondition> conditions, Map<String, Object> fieldValues) {
        return conditions.stream().allMatch(condition -> matches(condition, fieldValues));
    }

    private boolean matches(ApprovalStepCondition condition, Map<String, Object> fieldValues) {
        Object actual = fieldValues.get(condition.getFieldName());
        if (actual == null) {
            return false;
        }
        if (condition.getValueNumber() != null) {
            BigDecimal actualNumber = toBigDecimal(actual);
            return actualNumber != null && compare(actualNumber.compareTo(condition.getValueNumber()), condition.getOperator());
        }
        String actualText = String.valueOf(actual);
        return compare(actualText.compareTo(condition.getValueString()), condition.getOperator());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return null;
    }

    private boolean compare(int comparison, com.eudext.erp.workflow.internal.chain.ConditionOperator operator) {
        return switch (operator) {
            case EQ -> comparison == 0;
            case NE -> comparison != 0;
            case GT -> comparison > 0;
            case GTE -> comparison >= 0;
            case LT -> comparison < 0;
            case LTE -> comparison <= 0;
        };
    }
}
