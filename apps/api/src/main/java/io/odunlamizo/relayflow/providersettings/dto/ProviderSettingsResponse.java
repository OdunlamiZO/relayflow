package io.odunlamizo.relayflow.providersettings.dto;

// Docs: docs/codebase-components.md#provider-settings-dto-records

public record ProviderSettingsResponse(
        ProviderTypeSettingsResponse chatModel,
        ProviderTypeSettingsResponse transcription,
        ProviderTypeSettingsResponse storage) {}
