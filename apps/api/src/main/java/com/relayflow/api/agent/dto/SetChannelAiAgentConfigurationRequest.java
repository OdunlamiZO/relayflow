package com.relayflow.api.agent.dto;

import java.util.UUID;

/** {@code configurationId} null means the channel uses the workspace's default config. */
public record SetChannelAiAgentConfigurationRequest(UUID configurationId) {}
