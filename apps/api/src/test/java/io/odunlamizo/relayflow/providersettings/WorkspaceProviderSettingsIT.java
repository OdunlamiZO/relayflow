package io.odunlamizo.relayflow.providersettings;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;

import io.odunlamizo.relayflow.support.ApplicationIntegrationTest;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceProviderSettings;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WorkspaceProviderSettingsIT extends ApplicationIntegrationTest {

    @Autowired private WorkspaceRepository workspaceRepository;

    @Test
    void aWorkspaceWithoutSettingsUsesTheInstanceForEverything() {
        UUID workspaceId = workspace();

        assertThat(workspaceRepository.findById(workspaceId).orElseThrow().getProviderSettings())
                .isEqualTo(WorkspaceProviderSettings.NONE);
    }

    @Test
    void settingsAreSavedAndReadBack() {
        UUID workspaceId = workspace();
        WorkspaceProviderSettings settings =
                new WorkspaceProviderSettings(
                        "OPENAI_COMPATIBLE",
                        Map.of("OPENAI_COMPATIBLE", "llama-3.3-70b"),
                        "OPENAI_COMPATIBLE",
                        Map.of("OPENAI_COMPATIBLE", "whisper-large-v3-turbo"),
                        "LOCAL_DISK");
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        workspace.setProviderSettings(settings);
        workspaceRepository.save(workspace);
        flushAndClear();

        assertThat(workspaceRepository.findById(workspaceId).orElseThrow().getProviderSettings())
                .isEqualTo(settings);
    }
}
