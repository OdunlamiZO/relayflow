package io.odunlamizo.relayflow.workflow.engine;

// Docs: docs/codebase-components.md#voicereplyconverter

import io.odunlamizo.relayflow.agent.llm.AgentLlmRequest;
import io.odunlamizo.relayflow.agent.llm.AgentLlmResponse;
import io.odunlamizo.relayflow.agent.llm.LlmClientFactory;
import io.odunlamizo.relayflow.agent.llm.LlmMessage;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceProviderSettings;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Asks the chat model to turn a rejected voice reply into the value its hook expects. */
@Component
public class VoiceReplyConverter {

    static final String CONVERSION_PROMPT =
            "You convert a customer's answer, transcribed from a voice message, into the format a"
                    + " question requires, as described by the reason it was rejected. Put only"
                    + " the converted value in the reply, or leave the reply empty if it can't be"
                    + " converted without guessing.";

    private final LlmClientFactory llmClientFactory;

    private final WorkspaceRepository workspaceRepository;

    public VoiceReplyConverter(
            LlmClientFactory llmClientFactory, WorkspaceRepository workspaceRepository) {
        this.llmClientFactory = llmClientFactory;
        this.workspaceRepository = workspaceRepository;
    }

    public Optional<String> convert(
            UUID workspaceId, String question, String transcript, String reason) {
        WorkspaceProviderSettings settings =
                workspaceRepository
                        .findById(workspaceId)
                        .map(Workspace::getProviderSettings)
                        .orElse(WorkspaceProviderSettings.NONE);
        String request =
                "Question: "
                        + (question != null ? question : "")
                        + "\nCustomer's answer: \""
                        + transcript
                        + "\"\nRejected with: "
                        + reason;
        AgentLlmResponse response =
                llmClientFactory
                        .getClient(settings, null)
                        .complete(
                                new AgentLlmRequest(
                                        CONVERSION_PROMPT,
                                        List.of(LlmMessage.user(request)),
                                        null,
                                        List.of()));
        String value = response.reply() != null ? response.reply().trim() : "";

        if (response.failed() || value.isEmpty() || value.equals(transcript.trim())) {
            return Optional.empty();
        }

        return Optional.of(value);
    }
}
