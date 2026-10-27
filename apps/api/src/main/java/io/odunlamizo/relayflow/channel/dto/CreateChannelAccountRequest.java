package io.odunlamizo.relayflow.channel.dto;

import io.odunlamizo.relayflow.channel.domain.ChannelAccountStatus;
import io.odunlamizo.relayflow.channel.domain.ChannelProvider;
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
