package com.relayflow.api.workflow.engine;

import com.relayflow.api.workflow.NodeType;

/**
 * Strategy interface implemented by each node type's executor.
 *
 * <p>Implementations are Spring beans annotated with {@code @Component}. The engine selects the
 * right executor by matching {@link #nodeType()} against the node's type string.
 */
public interface NodeExecutor {

    /** The {@link NodeType} this executor handles. */
    NodeType nodeType();

    /**
     * Executes the node and returns the result indicating which edge handle to follow next.
     *
     * @throws NodeExecutionException if execution fails in a non-recoverable way
     */
    NodeExecutionResult execute(GraphNode node, ExecutionContext context);
}
