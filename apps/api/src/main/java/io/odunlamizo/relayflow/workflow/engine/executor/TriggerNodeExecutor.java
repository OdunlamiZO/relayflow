package io.odunlamizo.relayflow.workflow.engine.executor;

// Docs: docs/codebase-components.md#triggernodeexecutor

import io.odunlamizo.relayflow.workflow.NodeType;
import io.odunlamizo.relayflow.workflow.engine.ExecutionContext;
import io.odunlamizo.relayflow.workflow.engine.GraphNode;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutionResult;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutor;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TriggerNodeExecutor implements NodeExecutor {

    @Override
    public NodeType nodeType() {
        return NodeType.TRIGGER;
    }

    @Override
    public NodeExecutionResult execute(GraphNode node, ExecutionContext context) {
        return NodeExecutionResult.next(Map.of("triggerEvent", "conversation_opened"));
    }
}
