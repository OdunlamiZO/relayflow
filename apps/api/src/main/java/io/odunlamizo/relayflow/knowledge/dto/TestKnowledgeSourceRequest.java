package io.odunlamizo.relayflow.knowledge.dto;

// Docs: docs/codebase-components.md#knowledge-dto-records

import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSourceHeader;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TestKnowledgeSourceRequest(
        @NotBlank @Size(max = 2000) String url,
        @Size(max = 20) List<@Valid KnowledgeSourceHeader> headers) {}
