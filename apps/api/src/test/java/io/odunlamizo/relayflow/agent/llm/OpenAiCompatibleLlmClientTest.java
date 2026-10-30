package io.odunlamizo.relayflow.agent.llm;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiCompatibleLlmClientTest {

    private static final String COMPLETION =
            """
            {"choices":[{"message":{"content":"{\\"reply\\":\\"Hello\\",\\"confidence\\":\\"high\\"}"}}]}
            """;

    private final LlmPlatformConfigService configService = mock(LlmPlatformConfigService.class);

    private RestClient.Builder restClientBuilder;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void postsToTheConfiguredBaseUrlWithKeyAndModel() {
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(header("Authorization", "Bearer gsk_test"))
                .andExpect(jsonPath("$.model").value("llama-3.3-70b-versatile"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.provider").doesNotExist())
                .andRespond(withSuccess(COMPLETION, MediaType.APPLICATION_JSON));

        AgentLlmResponse response =
                client("https://api.groq.com/openai/v1/", "gsk_test", "llama-3.3-70b-versatile")
                        .complete(request());

        server.verify();
        assertThat(response.failed()).isFalse();
        assertThat(response.reply()).isEqualTo("Hello");
    }

    @Test
    void asksOpenRouterToRouteOnlyToHostsSupportingJsonOutput() {
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(jsonPath("$.provider.require_parameters").value(true))
                .andRespond(withSuccess(COMPLETION, MediaType.APPLICATION_JSON));

        client("https://openrouter.ai/api/v1", "sk-or-test", "openai/gpt-4o-mini")
                .complete(request());

        server.verify();
    }

    @Test
    void sendsNoAuthorizationHeaderWithoutAKey() {
        server.expect(requestTo("http://localhost:11434/v1/chat/completions"))
                .andExpect(
                        request ->
                                assertThat(request.getHeaders().containsKey("Authorization"))
                                        .isFalse())
                .andRespond(withSuccess(COMPLETION, MediaType.APPLICATION_JSON));

        client("http://localhost:11434/v1", "", "llama3.2").complete(request());

        server.verify();
    }

    @Test
    void failsWithoutCallingAnythingWhenNoModelIsSet() {
        AgentLlmResponse response = client("http://localhost:11434/v1", "", "").complete(request());

        server.verify();
        assertThat(response.failed()).isTrue();
    }

    @Test
    void failsWithoutCallingAnythingWhenNoBaseUrlIsSet() {
        AgentLlmResponse response = client("", "", "llama3.2").complete(request());

        server.verify();
        assertThat(response.failed()).isTrue();
    }

    @Test
    void failsWhenTheApiErrors() {
        server.expect(requestTo("http://localhost:11434/v1/chat/completions"))
                .andRespond(withServerError());

        AgentLlmResponse response =
                client("http://localhost:11434/v1", "", "llama3.2").complete(request());

        assertThat(response.failed()).isTrue();
    }

    /** {@code model} is what Redis holds; empty means the key isn't set. */
    private OpenAiCompatibleLlmClient client(String baseUrl, String apiKey, String model) {
        when(configService.getConfigValue("platform:llm:openai-compatible:model", ""))
                .thenReturn(model);

        return new OpenAiCompatibleLlmClient(
                new ObjectMapper(), configService, restClientBuilder, baseUrl, apiKey);
    }

    private static AgentLlmRequest request() {
        return new AgentLlmRequest(
                "You are helpful.", List.of(LlmMessage.user("Hi")), null, List.of());
    }
}
