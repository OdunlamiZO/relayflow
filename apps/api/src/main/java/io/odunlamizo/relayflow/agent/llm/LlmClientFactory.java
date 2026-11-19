package io.odunlamizo.relayflow.agent.llm;

// Docs: docs/codebase-components.md#llmclientfactory

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.plugin.ChatModelProvider;
import io.odunlamizo.relayflow.pluginregistry.PluginRegistry;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceProviderSettings;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LlmClientFactory {

    private static final Logger log = LoggerFactory.getLogger(LlmClientFactory.class);

    private final PluginRegistry pluginRegistry;

    private final LlmPlatformConfigService configService;

    private final ObjectMapper objectMapper;

    public LlmClientFactory(
            PluginRegistry pluginRegistry,
            LlmPlatformConfigService configService,
            ObjectMapper objectMapper) {
        this.pluginRegistry = pluginRegistry;
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

    public LlmClient getClient(WorkspaceProviderSettings settings, String agentProvider) {
        ChatModelProvider provider =
                installed(agentProvider, "agent")
                        .or(() -> installed(settings.llmProvider(), "workspace"))
                        .or(this::instanceProvider)
                        .orElse(null);
        String model = provider != null ? settings.llmModel(provider.id()).orElse(null) : null;

        return new LlmClient(provider, model, configService, objectMapper);
    }

    public Optional<ChatModelProvider> instanceProvider() {
        return configService
                .getActiveProvider()
                .flatMap(providerId -> installed(providerId, "instance"));
    }

    private Optional<ChatModelProvider> installed(String providerId, String chosenBy) {
        if (providerId == null) {
            return Optional.empty();
        }

        Optional<ChatModelProvider> provider = pluginRegistry.findChatModelProvider(providerId);

        if (provider.isEmpty()) {
            log.warn(
                    "LLM provider plugin isn't installed, falling back {} {}",
                    kv("chosenBy", chosenBy),
                    kv("provider", providerId));
        }

        return provider;
    }
}
