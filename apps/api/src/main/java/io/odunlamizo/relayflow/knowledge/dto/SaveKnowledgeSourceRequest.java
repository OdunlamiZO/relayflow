package io.odunlamizo.relayflow.knowledge.dto;

// Docs: docs/codebase-components.md#knowledge-dto-records

import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSourceHeader;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveKnowledgeSourceRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$") String name,
        @NotBlank @Size(max = 2000) String url,
        @Size(max = 20) List<@Valid KnowledgeSourceHeader> headers,
        @Min(0) @Max(3600) Integer cacheSeconds) {}
