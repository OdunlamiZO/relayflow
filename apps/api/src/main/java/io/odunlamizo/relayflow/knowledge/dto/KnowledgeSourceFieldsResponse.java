package io.odunlamizo.relayflow.knowledge.dto;

// Docs: docs/codebase-components.md#knowledge-dto-records

import java.util.List;

/** {@code error} is set when the source couldn't be fetched; {@code fields} is then empty. */
public record KnowledgeSourceFieldsResponse(List<KnowledgeSourceField> fields, String error) {}
