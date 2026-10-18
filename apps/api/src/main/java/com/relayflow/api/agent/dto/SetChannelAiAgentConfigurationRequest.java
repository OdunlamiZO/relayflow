package com.relayflow.api.agent.dto;

import java.util.UUID;

/** A null configurationId means the channel uses the workspace's default configuration. */
public record SetChannelAiAgentConfigurationRequest(UUID configurationId) {}
