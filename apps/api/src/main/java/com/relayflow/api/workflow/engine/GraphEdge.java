package com.relayflow.api.workflow.engine;

/**
 * A parsed edge from the React Flow {@code draftGraph}.
 *
 * @param id the edge's unique ID
 * @param source source node ID
 * @param target target node ID
 * @param sourceHandle the handle on the source node (e.g. {@code "true"}, {@code "success"}); null
 *     for single-output nodes
 */
public record GraphEdge(String id, String source, String target, String sourceHandle) {}
