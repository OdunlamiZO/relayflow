package io.odunlamizo.relayflow.providersettings.dto;

// Docs: docs/codebase-components.md#provider-settings-dto-records

public record ProviderOptionResponse(
        String id,
        String displayName,
        String configurationProblem,
        String model,
        String instanceModel) {}
