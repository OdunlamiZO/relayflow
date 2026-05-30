package com.relayflow.api.workflow.engine.executor;

import com.relayflow.api.workflow.NodeType;
import com.relayflow.api.workflow.engine.ExecutionContext;
import com.relayflow.api.workflow.engine.GraphNode;
import com.relayflow.api.workflow.engine.NodeExecutionResult;
import com.relayflow.api.workflow.engine.NodeExecutor;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Sets (or overwrites) a named variable in the execution context. Variables are always
 * reassignable.
 */
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
