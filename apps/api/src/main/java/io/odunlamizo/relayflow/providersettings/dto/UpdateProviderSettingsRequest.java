package io.odunlamizo.relayflow.providersettings.dto;

// Docs: docs/codebase-components.md#provider-settings-dto-records

import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpdateProviderSettingsRequest(
        @Size(max = 40) String llmProvider,
        @Size(max = 20) Map<@Size(max = 40) String, @Size(max = 200) String> llmModels,
        @Size(max = 40) String transcriptionProvider,
        @Size(max = 20) Map<@Size(max = 40) String, @Size(max = 200) String> transcriptionModels,
        @Size(max = 40) String storageProvider) {}
