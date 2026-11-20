package io.odunlamizo.relayflow.knowledge.dto;

// Docs: docs/codebase-components.md#knowledge-dto-records

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PreviewKnowledgeRequest(@NotNull @Size(max = 20000) String text) {}
