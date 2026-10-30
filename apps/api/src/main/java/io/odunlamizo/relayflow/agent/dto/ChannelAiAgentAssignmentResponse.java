package io.odunlamizo.relayflow.agent.dto;

// Docs: docs/codebase-components.md#ai-agent-dto-records

import java.util.UUID;

/** A null configurationId means the channel uses the workspace's default configuration. */
public record ChannelAiAgentAssignmentResponse(UUID configurationId) {}
