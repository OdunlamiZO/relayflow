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
        LlmProvider provider = configService.getActiveProvider();

        return clients.getOrDefault(provider, clients.get(LlmProvider.ANTHROPIC));
    }
}
