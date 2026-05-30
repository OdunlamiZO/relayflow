package com.relayflow.api.workflow.engine.executor;

import com.relayflow.api.workflow.NodeType;
import com.relayflow.api.workflow.engine.ExecutionContext;
import com.relayflow.api.workflow.engine.GraphNode;
import com.relayflow.api.workflow.engine.NodeExecutionException;
import com.relayflow.api.workflow.engine.NodeExecutionResult;
import com.relayflow.api.workflow.engine.NodeExecutor;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Executor for {@code jumpTo} nodes.
 *
 * <p>Redirects execution to a target node by id, bypassing normal edge traversal. A per-node jump
 * counter stored in the execution context enforces the configured {@code maxJumps} limit — once
 * reached the walk terminates rather than looping forever.
 */
@Component
public class JumpToNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(JumpToNodeExecutor.class);

    private static final String JUMP_COUNT_PREFIX = "__jumpCount_";

    @Override
    public NodeType nodeType() {
        return NodeType.JUMP_TO;
    }

    @Override
    public NodeExecutionResult execute(GraphNode node, ExecutionContext context) {
        String targetNodeId = (String) node.data().get("targetNodeId");

        if (targetNodeId == null || targetNodeId.isBlank()) {
            throw new NodeExecutionException("Jump To node has no target configured");
        }

        int maxJumps = ((Number) node.data().getOrDefault("maxJumps", 1)).intValue();
        String jumpKey = JUMP_COUNT_PREFIX + node.id();
        Object rawCount = context.getVariables().get(jumpKey);
        int jumpCount = rawCount instanceof Number n ? n.intValue() : 0;

        if (jumpCount >= maxJumps) {
            // Limit reached — end this branch normally instead of looping.
            log.warn("Jump limit reached: nodeId={}, maxJumps={}", node.id(), maxJumps);

            return NodeExecutionResult.next(Map.of("jumpCount", jumpCount, "limitReached", true));
        }

        context.setVariable(jumpKey, jumpCount + 1);

        log.debug(
                "Jumping: from={}, to={}, count={}/{}",
                node.id(),
                targetNodeId,
                jumpCount + 1,
                maxJumps);

        return NodeExecutionResult.jumpTo(
                targetNodeId, Map.of("targetNodeId", targetNodeId, "jumpCount", jumpCount + 1));
    }
}
