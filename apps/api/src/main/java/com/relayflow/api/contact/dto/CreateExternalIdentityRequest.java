package com.relayflow.api.contact.dto;

import com.relayflow.api.channel.domain.ChannelProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record CreateExternalIdentityRequest(
        @NotNull UUID workspaceId,
        @NotNull UUID contactId,
        UUID channelAccountId,
        @NotNull ChannelProvider provider,
        @NotBlank String externalUserId,
        String externalConversationId,
        String username,
        Map<String, Object> rawProfile) {}
