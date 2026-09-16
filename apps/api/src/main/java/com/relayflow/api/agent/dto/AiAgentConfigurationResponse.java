package com.relayflow.api.agent.dto;

import com.relayflow.api.agent.domain.AutonomyCeiling;
import com.relayflow.api.agent.domain.ExtractionField;
import com.relayflow.api.agent.domain.KnowledgeEntry;
import com.relayflow.api.agent.domain.WorkflowMapping;
import com.relayflow.api.agent.llm.LlmProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiAgentConfigurationResponse(
        UUID id,
        UUID workspaceId,
        String name,
        boolean isDefault,
        boolean enabled,
        AutonomyCeiling autonomyCeiling,
        LlmProvider llmProvider,
        String instructions,
        List<KnowledgeEntry> knowledgeBase,
        List<String> escalationKeywords,
        List<WorkflowMapping> workflowMappings,
        List<ExtractionField> extractionFields,
        Instant createdAt,
        Instant updatedAt) {}
