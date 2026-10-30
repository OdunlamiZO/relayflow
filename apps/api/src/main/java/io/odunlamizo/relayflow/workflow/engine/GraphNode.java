package io.odunlamizo.relayflow.workflow.engine;

// Docs: docs/codebase-components.md#graphnode

import java.util.Map;

public record GraphNode(String id, String type, Map<String, Object> data) {}
