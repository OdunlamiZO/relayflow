package com.relayflow.api.profile.dto;

import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String email,
        String displayName,
        String avatarUrl,
        boolean twoFactorEnabled,
        List<String> providers) {}
