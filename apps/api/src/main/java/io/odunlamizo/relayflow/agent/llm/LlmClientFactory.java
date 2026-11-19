package io.odunlamizo.relayflow.agent.llm;

// Docs: docs/codebase-components.md#llmclientfactory

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.plugin.ChatModelProvider;
import io.odunlamizo.relayflow.pluginregistry.PluginRegistry;
import java.util.List;
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

    public LlmClient getActiveClient() {
        return getClient(null);
    }

    /**
     * Uses the override when its plugin is installed, then the platform's active provider, then the
     * first installed chat model plugin.
     */
    public LlmClient getClient(String providerOverride) {
        if (providerOverride != null) {
            var override = pluginRegistry.findChatModelProvider(providerOverride);

            if (override.isPresent()) {
                return new LlmClient(override.get(), configService, objectMapper);
            }

            log.warn(
                    "Agent's LLM provider plugin isn't installed, using the platform provider {}",
                    kv("provider", providerOverride));
        }

        var active =
                configService.getActiveProvider().flatMap(pluginRegistry::findChatModelProvider);

        if (active.isPresent()) {
            return new LlmClient(active.get(), configService, objectMapper);
        }

        List<ChatModelProvider> installed = pluginRegistry.chatModelProviders();

        return new LlmClient(
                installed.isEmpty() ? null : installed.getFirst(), configService, objectMapper);
    }
}
