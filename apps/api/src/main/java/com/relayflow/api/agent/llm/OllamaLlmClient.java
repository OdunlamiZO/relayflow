package com.relayflow.api.agent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaLlmClient extends AbstractOpenAiCompatibleLlmClient {

    private static final String DEFAULT_MODEL = "llama3.2";

    private static final String DEFAULT_URL = "http://localhost:11434";

    private static final String MODEL_REDIS_KEY = "platform:llm:ollama:model";

    private static final String URL_REDIS_KEY = "platform:llm:ollama:url";

    private final LlmPlatformConfigService configService;

    public OllamaLlmClient(ObjectMapper objectMapper, LlmPlatformConfigService configService) {
        super(objectMapper);
        this.configService = configService;
    }

    @Override
    public LlmProvider provider() {

        return LlmProvider.OLLAMA;
    }

    @Override
    public AgentLlmResponse complete(AgentLlmRequest request) {
        String model =
                (request.model() != null && !request.model().isBlank())
                        ? request.model()
                        : configService.getConfigValue(MODEL_REDIS_KEY, DEFAULT_MODEL);
        String baseUrl = configService.getConfigValue(URL_REDIS_KEY, DEFAULT_URL);

        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();

        return doComplete(restClient, model, request);
    }
}
