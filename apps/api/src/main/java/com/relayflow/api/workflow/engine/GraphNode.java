package com.relayflow.api.workflow.engine;

import java.util.Map;

/**
 * A parsed node from the React Flow {@code draftGraph}.
 *
 * @param id the node's unique ID within the graph (e.g. {@code "trigger-1"})
 * @param type the node type string (e.g. {@code "trigger"}, {@code "sendMessage"})
 * @param data the node's data object as a raw map — each executor casts the fields it needs
 */
public record GraphNode(String id, String type, Map<String, Object> data) {}
