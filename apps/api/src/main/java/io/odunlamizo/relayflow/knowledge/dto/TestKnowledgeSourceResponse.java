package io.odunlamizo.relayflow.knowledge.dto;

// Docs: docs/codebase-components.md#knowledge-dto-records

/** {@code body} is the formatted JSON response; {@code error} is set when the fetch failed. */
public record TestKnowledgeSourceResponse(Integer statusCode, String body, String error) {}
