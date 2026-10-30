package io.odunlamizo.relayflow.publicapi;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class PublicApiAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final PublicApiAuthenticationEntryPoint entryPoint =
            new PublicApiAuthenticationEntryPoint(new PublicApiProblemWriter(objectMapper));

    @Test
    void missingKeyIsAProblem() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/public/v1/conversations");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("none"));

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(body.get("code").asText()).isEqualTo("MISSING_API_KEY");
        assertThat(body.get("type").asText()).isEqualTo("/problems/missing-api-key");
        assertThat(body.get("instance").asText()).isEqualTo("/public/v1/conversations");
    }

    @Test
    void rejectedKeyIsAProblem() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/public/v1/conversations");
        request.addHeader("X-Api-Key", "rf_revoked");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("none"));

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("code").asText()).isEqualTo("INVALID_API_KEY");
        assertThat(body.get("detail").asText())
                .isEqualTo("The API key is unknown, revoked, or expired.");
    }
}
