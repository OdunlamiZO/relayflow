package com.relayflow.api.workflow.engine;

import com.relayflow.api.workflow.NodeType;

public interface NodeExecutor {

    NodeType nodeType();

    NodeExecutionResult execute(GraphNode node, ExecutionContext context);
}
