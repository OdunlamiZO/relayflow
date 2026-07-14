package com.relayflow.api.messaging.dto;

import com.relayflow.api.messaging.domain.ChannelAccountStatus;
import com.relayflow.api.messaging.domain.ChannelProvider;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ChannelAccountResponse(
        UUID id,
        UUID workspaceId,
        ChannelProvider provider,
        String name,
        ChannelAccountStatus status,
        Map<String, Object> metadata,
        Instant createdAt) {}
