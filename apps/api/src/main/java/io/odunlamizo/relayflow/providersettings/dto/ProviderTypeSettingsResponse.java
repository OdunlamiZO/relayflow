package io.odunlamizo.relayflow.providersettings.dto;

// Docs: docs/codebase-components.md#provider-settings-dto-records

import java.util.List;

public record ProviderTypeSettingsResponse(
        String selected, String instanceDefault, List<ProviderOptionResponse> providers) {}
