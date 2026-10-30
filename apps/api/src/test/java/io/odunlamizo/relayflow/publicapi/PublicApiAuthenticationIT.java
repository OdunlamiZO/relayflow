package io.odunlamizo.relayflow.publicapi;

// Docs: docs/codebase-components.md#backend-tests

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Runs through the real security filter chain, which the controller test skips. */
@SpringBootTest(properties = "relayflow.ratelimit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PublicApiAuthenticationIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;

    @Test
    void missingKeyIsAProblem() throws Exception {
        mockMvc.perform(get("/public/v1/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MISSING_API_KEY"));
    }

    @Test
    void unknownKeyIsAProblem() throws Exception {
        mockMvc.perform(get("/public/v1/conversations").header("X-Api-Key", "rf_unknown"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"));
    }

    @Test
    void unknownEndpointWithoutKeyIsUnauthorized() throws Exception {
        mockMvc.perform(get("/public/v1/invoices"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MISSING_API_KEY"));
    }
}
