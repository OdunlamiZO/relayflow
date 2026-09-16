package com.relayflow.api.agent.dto;

import com.relayflow.api.agent.domain.AutonomyCeiling;
import com.relayflow.api.agent.domain.ExtractionField;
import com.relayflow.api.agent.domain.KnowledgeEntry;
import com.relayflow.api.agent.domain.WorkflowMapping;
import com.relayflow.api.agent.llm.LlmProvider;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public record UpdateAiAgentConfigurationRequest(
        String name,
        @NotNull Boolean enabled,
        @NotNull AutonomyCeiling autonomyCeiling,
        /** Null means this agent uses the platform's active LLM provider. */
        LlmProvider llmProvider,
        String instructions,
        List<KnowledgeEntry> knowledgeBase,
        List<String> escalationKeywords,
        List<WorkflowMapping> workflowMappings,
        List<ExtractionField> extractionFields) {

    public UpdateAiAgentConfigurationRequest {
        if (knowledgeBase == null) knowledgeBase = new ArrayList<>();
        if (escalationKeywords == null) escalationKeywords = new ArrayList<>();
        if (workflowMappings == null) workflowMappings = new ArrayList<>();
        if (extractionFields == null) extractionFields = new ArrayList<>();
    }
}
