package io.odunlamizo.relayflow.knowledge.dto;

// Docs: docs/codebase-components.md#knowledge-dto-records

import java.util.List;

public record PreviewKnowledgeResponse(String text, List<String> failures) {}
