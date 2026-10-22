package io.odunlamizo.relayflow.workflow.engine.executor;

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.workflow.NodeType;
import io.odunlamizo.relayflow.workflow.engine.ExecutionContext;
import io.odunlamizo.relayflow.workflow.engine.GraphNode;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutionException;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutionResult;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutor;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
            log.warn("Jump limit reached {} {}", kv("nodeId", node.id()), kv("maxJumps", maxJumps));

            return NodeExecutionResult.next(Map.of("jumpCount", jumpCount, "limitReached", true));
        }

        context.setVariable(jumpKey, jumpCount + 1);

        log.debug(
                "Jumping to node {} {} {} {}",
                kv("fromNodeId", node.id()),
                kv("toNodeId", targetNodeId),
                kv("jumpCount", jumpCount + 1),
                kv("maxJumps", maxJumps));

        return NodeExecutionResult.jumpTo(
                targetNodeId, Map.of("targetNodeId", targetNodeId, "jumpCount", jumpCount + 1));
    }
}
