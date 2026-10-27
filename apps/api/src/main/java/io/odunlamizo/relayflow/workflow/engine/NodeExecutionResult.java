package io.odunlamizo.relayflow.workflow.engine;

import java.util.Map;

public record NodeExecutionResult(
        String nextHandle,
        Map<String, Object> output,
        boolean waiting,
        String jumpToNodeId,
        Long timeoutSeconds) {

    public static NodeExecutionResult next(Map<String, Object> output) {
        return new NodeExecutionResult(null, output, false, null, null);
    }

    public static NodeExecutionResult handle(String handle, Map<String, Object> output) {
        return new NodeExecutionResult(handle, output, false, null, null);
    }

    public static NodeExecutionResult waiting(Map<String, Object> output, long timeoutSeconds) {
        return new NodeExecutionResult(null, output, true, null, timeoutSeconds);
    }

    public static NodeExecutionResult jumpTo(String nodeId, Map<String, Object> output) {
        return new NodeExecutionResult(null, output, false, nodeId, null);
    }
}
