package com.relayflow.api.agent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GroqLlmClient extends AbstractOpenAiCompatibleLlmClient {

    private static final String DEFAULT_MODEL = "llama-3.1-8b-instant";

    private static final String MODEL_REDIS_KEY = "platform:llm:groq:model";

    @Value("${relayflow.llm.groq.api-key}")
    private String apiKey;

    private final LlmPlatformConfigService configService;

    private RestClient restClient;

    public GroqLlmClient(ObjectMapper objectMapper, LlmPlatformConfigService configService) {
        super(objectMapper);
        this.configService = configService;
    }

    @PostConstruct
    void init() {
        restClient =
                RestClient.builder()
                        .baseUrl("https://api.groq.com/openai")
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .build();
    }

    @Override
    public LlmProvider provider() {

        return LlmProvider.GROQ;
    }

    @Override
    public AgentLlmResponse complete(AgentLlmRequest request) {
        String model =
                (request.model() != null && !request.model().isBlank())
                        ? request.model()
                        : configService.getConfigValue(MODEL_REDIS_KEY, DEFAULT_MODEL);

        return doComplete(restClient, model, request);
    }
}
