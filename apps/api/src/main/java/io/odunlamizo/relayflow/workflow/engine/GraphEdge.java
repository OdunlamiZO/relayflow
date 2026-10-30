package io.odunlamizo.relayflow.workflow.engine;

// Docs: docs/codebase-components.md#graphedge

public record GraphEdge(String id, String source, String target, String sourceHandle) {}
