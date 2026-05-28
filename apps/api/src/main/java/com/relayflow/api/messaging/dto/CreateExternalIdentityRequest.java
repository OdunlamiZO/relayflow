package com.relayflow.api.messaging.dto;

import com.relayflow.api.messaging.domain.ChannelProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record CreateExternalIdentityRequest(
        @NotNull UUID workspaceId,
        @NotNull UUID contactId,
        @NotNull ChannelProvider provider,
        @NotBlank String externalUserId,
        String externalConversationId,
        String username,
        Map<String, Object> rawProfile) {}
