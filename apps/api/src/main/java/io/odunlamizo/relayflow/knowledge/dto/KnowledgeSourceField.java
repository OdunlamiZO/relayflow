package io.odunlamizo.relayflow.knowledge.dto;

// Docs: docs/codebase-components.md#knowledge-dto-records

/**
 * {@code path} goes after the source name in a placeholder; {@code sample} is its current value.
 */
public record KnowledgeSourceField(String path, String sample) {}
