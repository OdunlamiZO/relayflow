package io.odunlamizo.relayflow.agent.dto;

import java.util.UUID;

/** A null configurationId means the channel uses the workspace's default configuration. */
public record ChannelAiAgentAssignmentResponse(UUID configurationId) {}
