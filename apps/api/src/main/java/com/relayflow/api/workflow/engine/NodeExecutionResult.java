package com.relayflow.api.workflow.engine;

import java.util.Map;

/**
 * The result returned by a node executor after it runs.
 *
 * @param nextHandle the source handle whose outgoing edge should be followed; {@code null} means
 *     follow the default (un-labelled) edge, or end the run if none exists
 * @param output key-value pairs recorded in the step's output snapshot for observability
 * @param waiting {@code true} when the node has paused execution waiting for a reply; the engine
 *     will mark the run {@code WAITING} and stop walking
 * @param jumpToNodeId when non-null, the engine skips edge resolution entirely and jumps directly
 *     to the node with this id
 * @param timeoutSeconds when {@code waiting} is {@code true}, the number of seconds after which the
 *     run should be failed if no reply arrives; {@code null} means no timeout
 */
public record NodeExecutionResult(
        String nextHandle,
        Map<String, Object> output,
        boolean waiting,
        String jumpToNodeId,
        Long timeoutSeconds) {

    /** Use when the node has a single output and no branching. */
    public static NodeExecutionResult next(Map<String, Object> output) {
        return new NodeExecutionResult(null, output, false, null, null);
    }

    /** Use when the node branches via a named handle. */
    public static NodeExecutionResult handle(String handle, Map<String, Object> output) {
        return new NodeExecutionResult(handle, output, false, null, null);
    }

    /**
     * Use when the node pauses the run and waits for the contact to reply, failing the run after
     * {@code timeoutSeconds} if no reply arrives.
     */
    public static NodeExecutionResult waiting(Map<String, Object> output, long timeoutSeconds) {
        return new NodeExecutionResult(null, output, true, null, timeoutSeconds);
    }

    /** Use when the node must redirect execution directly to another node by id. */
    public static NodeExecutionResult jumpTo(String nodeId, Map<String, Object> output) {
        return new NodeExecutionResult(null, output, false, nodeId, null);
    }
}
