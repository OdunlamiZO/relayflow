package io.odunlamizo.relayflow.agent.llm;

// Docs: docs/codebase-components.md#llmclient

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.plugin.ChatMessage;
import io.odunlamizo.relayflow.plugin.ChatModelProvider;
import io.odunlamizo.relayflow.plugin.ChatRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final ChatModelProvider chatModelProvider;

    private final LlmPlatformConfigService configService;

    private final ObjectMapper objectMapper;

    public LlmClient(
            ChatModelProvider chatModelProvider,
            LlmPlatformConfigService configService,
            ObjectMapper objectMapper) {
        this.chatModelProvider = chatModelProvider;
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

    public static String modelKey(String providerId) {
        return "platform:llm:" + providerId.toLowerCase().replace('_', '-') + ":model";
    }

    /** Null when no chat model plugin is installed. */
    public String provider() {
        return chatModelProvider != null ? chatModelProvider.id() : null;
    }

    public AgentLlmResponse complete(AgentLlmRequest request) {
        if (chatModelProvider == null) {
            log.error("LLM call skipped, no chat model plugin is installed");

            return AgentLlmResponseParser.failure();
        }

        String model =
                request.model() != null && !request.model().isBlank()
                        ? request.model()
                        : configService.getConfigValue(modelKey(chatModelProvider.id()), null);
        String systemPrompt =
                request.systemPrompt()
                        + LlmPrompts.buildFormatInstruction(request.extractionFields());
        List<ChatMessage> messages =
                request.messages().stream()
                        .map(message -> new ChatMessage(message.role(), message.content()))
                        .toList();

        try {
            String reply =
                    chatModelProvider.complete(new ChatRequest(systemPrompt, messages, model));

            return AgentLlmResponseParser.parse(objectMapper, reply == null ? "" : reply);
        } catch (Exception exception) {
            log.error(
                    "LLM call failed {} {}",
                    kv("provider", chatModelProvider.id()),
                    kv("model", model),
                    exception);

            return AgentLlmResponseParser.failure();
        }
    }
}
