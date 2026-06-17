package com.relayflow.api.workflow.engine.executor;

import com.relayflow.api.workflow.NodeType;
import com.relayflow.api.workflow.engine.ExecutionContext;
import com.relayflow.api.workflow.engine.GraphNode;
import com.relayflow.api.workflow.engine.NodeExecutionResult;
import com.relayflow.api.workflow.engine.NodeExecutor;
import com.relayflow.api.workflow.engine.VariableInterpolator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Evaluates the first branch whose condition passes and returns its handle ID.
 *
 * <p>Branches are evaluated in order. The last branch acts as the fallback (its condition, if any,
 * is ignored once all prior branches fail). If no branch matches and no fallback exists, the run
 * ends at this node.
 */
@Component
public class ConditionNodeExecutor implements NodeExecutor {

    @Override
    public NodeType nodeType() {
        return NodeType.CONDITION;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeExecutionResult execute(GraphNode node, ExecutionContext context) {
        List<Map<String, Object>> branches =
                (List<Map<String, Object>>) node.data().get("branches");

        if (branches == null || branches.isEmpty()) {
            return NodeExecutionResult.next(Map.of("result", "no branches configured"));
        }

        for (int i = 0; i < branches.size(); i++) {
            Map<String, Object> branch = branches.get(i);
            String branchId = (String) branch.get("id");
            boolean isLastBranch = (i == branches.size() - 1);

            // Last branch is the fallback — always taken if nothing else matched.
            if (isLastBranch) {
                return NodeExecutionResult.handle(
                        branchId, Map.of("branch", branchId, "reason", "fallback"));
            }

            if (evaluate(branch, context)) {
                return NodeExecutionResult.handle(
                        branchId, Map.of("branch", branchId, "reason", "condition matched"));
            }
        }

        return NodeExecutionResult.next(Map.of("result", "no branch matched"));
    }

    /**
     * Evaluates a branch's conditions, combined with its {@code combinator} ("and"/"or", default
     * "and").
     */
    @SuppressWarnings("unchecked")
    private boolean evaluate(Map<String, Object> branch, ExecutionContext context) {
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) branch.get("conditions");

        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        boolean isOr = "or".equals(branch.get("combinator"));

        return isOr
                ? conditions.stream().anyMatch(condition -> evaluateCondition(condition, context))
                : conditions.stream().allMatch(condition -> evaluateCondition(condition, context));
    }

    private boolean evaluateCondition(Map<String, Object> condition, ExecutionContext context) {
        String variable = (String) condition.get("variable");
        String operator = (String) condition.get("operator");
        String value = (String) condition.get("value");

        if (variable == null || variable.isBlank() || operator == null) {
            return false;
        }

        String resolvedVar =
                VariableInterpolator.interpolate("{{" + variable + "}}", context.getVariables());
        String resolvedValue = value != null ? context.interpolate(value) : "";

        return switch (operator) {
            case "eq" -> resolvedVar.equals(resolvedValue);
            case "neq" -> !resolvedVar.equals(resolvedValue);
            case "gt" -> compareNumbers(resolvedVar, resolvedValue) > 0;
            case "lt" -> compareNumbers(resolvedVar, resolvedValue) < 0;
            case "gte" -> compareNumbers(resolvedVar, resolvedValue) >= 0;
            case "lte" -> compareNumbers(resolvedVar, resolvedValue) <= 0;
            case "contains" -> resolvedVar.contains(resolvedValue);
            case "not_contains" -> !resolvedVar.contains(resolvedValue);
            case "starts_with" -> resolvedVar.startsWith(resolvedValue);
            case "ends_with" -> resolvedVar.endsWith(resolvedValue);
            case "is_set" -> resolvedVar != null && !resolvedVar.isBlank();
            case "is_not_set" -> resolvedVar == null || resolvedVar.isBlank();
            default -> false;
        };
    }

    private int compareNumbers(String a, String b) {
        try {
            return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }
}
