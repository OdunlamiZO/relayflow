package io.odunlamizo.relayflow.knowledge.domain;

// Docs: docs/codebase-components.md#knowledgesource

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeSourceHeader(
        @NotBlank @Size(max = 200) String key, @Size(max = 2000) String value) {}
