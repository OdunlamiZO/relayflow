package io.odunlamizo.relayflow.workspace.domain;

// Docs: docs/codebase-components.md#workspaceprovidersettings

import java.util.Map;
import java.util.Optional;

public record WorkspaceProviderSettings(
        String llmProvider,
        Map<String, String> llmModels,
        String transcriptionProvider,
        Map<String, String> transcriptionModels,
        String storageProvider) {

    public static final WorkspaceProviderSettings NONE =
            new WorkspaceProviderSettings(null, Map.of(), null, Map.of(), null);

    public WorkspaceProviderSettings {
        if (llmModels == null) llmModels = Map.of();
        if (transcriptionModels == null) transcriptionModels = Map.of();
    }

    public Optional<String> llmModel(String providerId) {
        return Optional.ofNullable(llmModels.get(providerId));
    }

    public Optional<String> transcriptionModel(String providerId) {
        return Optional.ofNullable(transcriptionModels.get(providerId));
    }
}
