package com.relayflow.api.agent.llm;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LlmClientFactory {

    private final Map<LlmProvider, LlmClient> clients;

    private final LlmPlatformConfigService configService;

    public LlmClientFactory(List<LlmClient> clientList, LlmPlatformConfigService configService) {
        this.clients =
                clientList.stream()
                        .collect(Collectors.toMap(LlmClient::provider, client -> client));
        this.configService = configService;
    }

    public LlmClient getActiveClient() {
        return getClient(null);
    }

    /**
     * Returns the client for {@code providerOverride}, or the platform's active provider when
     * {@code providerOverride} is null — this is how an AI agent config with no explicit LLM
     * provider falls back to the platform default.
     */
    public LlmClient getClient(LlmProvider providerOverride) {
        LlmProvider provider =
                providerOverride != null ? providerOverride : configService.getActiveProvider();

        return clients.getOrDefault(provider, clients.get(LlmProvider.ANTHROPIC));
    }
}
