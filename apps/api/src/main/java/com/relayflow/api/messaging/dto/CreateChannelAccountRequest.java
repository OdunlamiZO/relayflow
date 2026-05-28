package com.relayflow.api.messaging.dto;

import com.relayflow.api.messaging.domain.ChannelAccountStatus;
import com.relayflow.api.messaging.domain.ChannelProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record CreateChannelAccountRequest(
        @NotNull UUID workspaceId,
        @NotNull ChannelProvider provider,
        @NotBlank String name,
        ChannelAccountStatus status,
        String encryptedCredentials,
        Map<String, Object> metadata) {}
