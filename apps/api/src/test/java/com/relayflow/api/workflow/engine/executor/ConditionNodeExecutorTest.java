package com.relayflow.api.workflow.engine.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.relayflow.api.workflow.engine.ExecutionContext;
import com.relayflow.api.workflow.engine.GraphNode;
import com.relayflow.api.workflow.engine.NodeExecutionResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConditionNodeExecutorTest {

    private final ConditionNodeExecutor executor = new ConditionNodeExecutor();

    @Test
    void treatsAMissingOperatorAsEquals() {
        NodeExecutionResult result =
                executor.execute(
                        conditionNode(Map.of("id", "c1", "variable", "plan", "value", "gold")),
                        context(Map.of("plan", "gold")));

        assertThat(result.nextHandle()).isEqualTo("true");
    }

    @Test
    void usesTheConfiguredOperator() {
        NodeExecutionResult result =
                executor.execute(
                        conditionNode(
                                Map.of(
                                        "id",
                                        "c1",
                                        "variable",
                                        "plan",
                                        "operator",
                                        "neq",
                                        "value",
                                        "gold")),
                        context(Map.of("plan", "gold")));

        assertThat(result.nextHandle()).isEqualTo("false");
    }

    private GraphNode conditionNode(Map<String, Object> condition) {
        return new GraphNode(
                "condition-1",
                "condition",
                Map.of(
                        "branches",
                        List.of(
                                Map.of(
                                        "id",
                                        "true",
                                        "label",
                                        "True",
                                        "conditions",
                                        List.of(condition)),
                                Map.of("id", "false", "label", "False"))));
    }

    private ExecutionContext context(Map<String, Object> variables) {
        return new ExecutionContext(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), variables);
    }
}
