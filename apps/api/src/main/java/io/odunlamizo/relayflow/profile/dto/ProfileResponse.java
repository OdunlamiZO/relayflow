package io.odunlamizo.relayflow.profile.dto;

// Docs: docs/codebase-components.md#profile-dto-records

import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String email,
        String displayName,
        String avatarUrl,
        boolean twoFactorEnabled,
        List<String> providers) {}
