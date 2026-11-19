package io.odunlamizo.relayflow.workflow.engine;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.odunlamizo.relayflow.agent.llm.AgentLlmRequest;
import io.odunlamizo.relayflow.agent.llm.AgentLlmResponse;
import io.odunlamizo.relayflow.agent.llm.LlmClient;
import io.odunlamizo.relayflow.agent.llm.LlmClientFactory;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceProviderSettings;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VoiceReplyConverterTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    @Mock private LlmClientFactory llmClientFactory;

    @Mock private WorkspaceRepository workspaceRepository;

    @Mock private LlmClient llmClient;

    @Test
    void usesTheWorkspaceChatModelAndReturnsTheConvertedValue() {
        WorkspaceProviderSettings settings =
                new WorkspaceProviderSettings("anthropic", Map.of(), null, Map.of(), null);
        Workspace workspace = new Workspace();
        workspace.setProviderSettings(settings);
        when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(workspace));
        when(llmClientFactory.getClient(settings, null)).thenReturn(llmClient);
        when(llmClient.complete(any())).thenReturn(response(" 5000 ", false));

        Optional<String> converted =
                converter()
                        .convert(
                                WORKSPACE_ID,
                                "How much?",
                                "Five thousand naira.",
                                "Please reply with a number.");

        assertThat(converted).contains("5000");

        ArgumentCaptor<AgentLlmRequest> request = ArgumentCaptor.forClass(AgentLlmRequest.class);
        verify(llmClient).complete(request.capture());
        assertThat(request.getValue().systemPrompt())
                .isEqualTo(VoiceReplyConverter.CONVERSION_PROMPT);
        assertThat(request.getValue().messages().getFirst().content())
                .contains("How much?")
                .contains("Five thousand naira.")
                .contains("Please reply with a number.");
    }

    @Test
    void returnsNothingWhenTheCallFails() {
        when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());
        when(llmClientFactory.getClient(WorkspaceProviderSettings.NONE, null))
                .thenReturn(llmClient);
        when(llmClient.complete(any())).thenReturn(response("", true));

        assertThat(converter().convert(WORKSPACE_ID, null, "Hello.", "Not a number")).isEmpty();
    }

    @Test
    void returnsNothingWhenTheValueIsUnchanged() {
        when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());
        when(llmClientFactory.getClient(WorkspaceProviderSettings.NONE, null))
                .thenReturn(llmClient);
        when(llmClient.complete(any())).thenReturn(response("Hello.", false));

        assertThat(converter().convert(WORKSPACE_ID, null, " Hello. ", "Not a number")).isEmpty();
    }

    private VoiceReplyConverter converter() {
        return new VoiceReplyConverter(llmClientFactory, workspaceRepository);
    }

    private AgentLlmResponse response(String reply, boolean failed) {
        return new AgentLlmResponse(reply, null, List.of(), false, false, Map.of(), failed);
    }
}
