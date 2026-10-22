package io.odunlamizo.relayflow.workflow.engine.executor;

import io.odunlamizo.relayflow.workflow.NodeType;
import io.odunlamizo.relayflow.workflow.engine.ExecutionContext;
import io.odunlamizo.relayflow.workflow.engine.GraphNode;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutionResult;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutor;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SetVariableNodeExecutor implements NodeExecutor {

    @Override
    public NodeType nodeType() {
        return NodeType.SET_VARIABLE;
    }

    @Override
    public NodeExecutionResult execute(GraphNode node, ExecutionContext context) {
        String variableName = (String) node.data().get("variableName");
        String rawValue = (String) node.data().get("value");

        if (variableName == null || variableName.isBlank()) {
            return NodeExecutionResult.next(Map.of("skipped", "no variable name configured"));
        }

        String resolvedValue = context.interpolate(rawValue != null ? rawValue : "");
        context.setVariable(variableName, resolvedValue);

        return NodeExecutionResult.next(Map.of(variableName, resolvedValue));
    }
}
