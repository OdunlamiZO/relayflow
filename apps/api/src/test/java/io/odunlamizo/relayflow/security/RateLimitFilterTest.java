package io.odunlamizo.relayflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.publicapi.PublicApiProblemWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private RateLimitFilter filter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void overTheLimit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString())).thenReturn(1_000L);

        filter =
                new RateLimitFilter(
                        redis,
                        objectMapper,
                        new PublicApiProblemWriter(objectMapper),
                        true,
                        5,
                        60,
                        10,
                        3600,
                        60,
                        60,
                        120,
                        60);
    }

    @Test
    void publicApiRejectionIsAProblem() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/public/v1/conversations");
        request.addHeader("X-Api-Key", "rf_key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(body.get("code").asText()).isEqualTo("RATE_LIMITED");
    }

    @Test
    void internalApiRejectionKeepsItsFormat() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(body.has("message")).isTrue();
        assertThat(body.has("code")).isFalse();
    }
}
