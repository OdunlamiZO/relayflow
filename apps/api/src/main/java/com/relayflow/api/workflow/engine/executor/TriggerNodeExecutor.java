package com.relayflow.api.workflow.engine.executor;

import com.relayflow.api.workflow.NodeType;
import com.relayflow.api.workflow.engine.ExecutionContext;
import com.relayflow.api.workflow.engine.GraphNode;
import com.relayflow.api.workflow.engine.NodeExecutionResult;
import com.relayflow.api.workflow.engine.NodeExecutor;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Entry-point node — no action, just passes execution to the next node. */
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
