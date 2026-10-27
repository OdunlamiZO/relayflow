package io.odunlamizo.relayflow.workflow.engine;

import io.odunlamizo.relayflow.workflow.NodeType;

public interface NodeExecutor {

    NodeType nodeType();

    NodeExecutionResult execute(GraphNode node, ExecutionContext context);
}
