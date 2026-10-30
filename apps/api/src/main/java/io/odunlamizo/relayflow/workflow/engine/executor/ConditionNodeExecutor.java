package io.odunlamizo.relayflow.workflow.engine.executor;

// Docs: docs/codebase-components.md#conditionnodeexecutor

import io.odunlamizo.relayflow.workflow.NodeType;
import io.odunlamizo.relayflow.workflow.engine.ExecutionContext;
import io.odunlamizo.relayflow.workflow.engine.GraphNode;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutionResult;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutor;
import io.odunlamizo.relayflow.workflow.engine.VariableInterpolator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

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

        for (int index = 0; index < branches.size(); index++) {
            Map<String, Object> branch = branches.get(index);
            String branchId = (String) branch.get("id");
            boolean isLastBranch = (index == branches.size() - 1);

            // The last branch is the fallback and is taken when nothing else matched.
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
        String operator =
                condition.get("operator") instanceof String configured && !configured.isBlank()
                        ? configured
                        : "eq";
        String value = (String) condition.get("value");

        if (variable == null || variable.isBlank()) {
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

    private int compareNumbers(String left, String right) {
        try {
            return Double.compare(Double.parseDouble(left), Double.parseDouble(right));
        } catch (NumberFormatException exception) {
            return left.compareTo(right);
        }
    }
}
