package io.odunlamizo.relayflow.knowledge.dto;

// Docs: docs/codebase-components.md#knowledge-dto-records

import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSourceHeader;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record KnowledgeSourceResponse(
        UUID id,
        String name,
        String url,
        List<KnowledgeSourceHeader> headers,
        int cacheSeconds,
        Instant createdAt,
        Instant updatedAt) {}
