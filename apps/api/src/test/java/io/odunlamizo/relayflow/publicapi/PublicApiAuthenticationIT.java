package io.odunlamizo.relayflow.publicapi;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.odunlamizo.relayflow.workspace.ApiKeyService;
import io.odunlamizo.relayflow.workspace.dto.CreateApiKeyRequest;
import io.odunlamizo.relayflow.workspace.dto.CreateApiKeyResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Runs through the real security filter chain, which the controller test skips. */
@SpringBootTest(properties = "relayflow.ratelimit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
class PublicApiAuthenticationIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;

    @Autowired private ApiKeyService apiKeyService;

    @Autowired private JdbcTemplate jdbc;

    @Autowired private jakarta.persistence.EntityManager entityManager;

    private UUID workspaceId;
    private UUID userId;

    @BeforeEach
    void createWorkspaceAndUser() {
        workspaceId = createWorkspace();
        userId = UUID.randomUUID();
        jdbc.update("insert into users (id, email) values (?, ?)", userId, userId + "@example.com");
    }

    @Test
    void createdKeyAuthenticatesAndPersistsOnlyItsHash() throws Exception {
        CreateApiKeyResponse key = createKey(null);

        assertThat(
                        jdbc.queryForObject(
                                "select key_hash from workspace_api_keys where id = ?",
                                String.class,
                                key.id()))
                .isEqualTo(ApiKeyService.sha256(key.key()))
                .isNotEqualTo(key.key());

        mockMvc.perform(
                        get("/public/v1/conversations")
                                .header("X-Api-Key", "  " + key.key() + "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.hasMore").value(false));

        assertThat(lastUsedAt(key.id())).isNotNull();
    }

    @Test
    void revokedKeyImmediatelyStopsAuthenticating() throws Exception {
        CreateApiKeyResponse key = createKey(null);
        mockMvc.perform(get("/public/v1/conversations").header("X-Api-Key", key.key()))
                .andExpect(status().isOk());

        apiKeyService.revokeApiKey(workspaceId, key.id());

        mockMvc.perform(get("/public/v1/conversations").header("X-Api-Key", key.key()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"));
        assertThat(apiKeyService.listApiKeys(workspaceId)).isEmpty();
    }

    @Test
    void expiredKeyIsRejectedWithoutRecordingUsage() throws Exception {
        CreateApiKeyResponse key = createKey(Instant.now().minus(1, ChronoUnit.DAYS));

        mockMvc.perform(get("/public/v1/conversations").header("X-Api-Key", key.key()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"));

        assertThat(lastUsedAt(key.id())).isNull();
    }

    @Test
    void recentUsageIsPreservedButStaleUsageIsRefreshed() throws Exception {
        CreateApiKeyResponse key = createKey(null);
        Instant recent = Instant.now().minus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        jdbc.update(
                "update workspace_api_keys set last_used_at = ? where id = ?",
                java.sql.Timestamp.from(recent),
                key.id());
        entityManager.clear();

        mockMvc.perform(get("/public/v1/conversations").header("X-Api-Key", key.key()))
                .andExpect(status().isOk());
        assertThat(lastUsedAt(key.id())).isEqualTo(recent);

        Instant stale = recent.minus(2, ChronoUnit.HOURS);
        jdbc.update(
                "update workspace_api_keys set last_used_at = ? where id = ?",
                java.sql.Timestamp.from(stale),
                key.id());
        // Clear the persistence context so the next request reads the database update.
        entityManager.clear();

        mockMvc.perform(get("/public/v1/conversations").header("X-Api-Key", key.key()))
                .andExpect(status().isOk());
        assertThat(lastUsedAt(key.id())).isAfter(recent);
    }

    @Test
    void anotherWorkspaceCannotRevokeKey() throws Exception {
        CreateApiKeyResponse key = createKey(null);

        assertThatThrownBy(() -> apiKeyService.revokeApiKey(createWorkspace(), key.id()))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void keyCannotReadAnotherWorkspacesConversation() throws Exception {
        CreateApiKeyResponse key = createKey(null);
        UUID otherWorkspace = createWorkspace();
        UUID contactId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        jdbc.update(
                "insert into contacts (id, workspace_id) values (?, ?)", contactId, otherWorkspace);
        jdbc.update(
                "insert into channel_accounts (id, workspace_id, provider, name, status)"
                        + " values (?, ?, 'TELEGRAM', 'Other bot', 'ACTIVE')",
                channelId,
                otherWorkspace);
        jdbc.update(
                "insert into conversations (id, workspace_id, contact_id, channel_account_id, status)"
                        + " values (?, ?, ?, ?, 'OPEN')",
                conversationId,
                otherWorkspace,
                contactId,
                channelId);

        mockMvc.perform(get("/public/v1/conversations").header("X-Api-Key", key.key()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
        mockMvc.perform(
                        get("/public/v1/conversations/" + conversationId)
                                .header("X-Api-Key", key.key()))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/public/v1/conversations/" + conversationId + "/messages")
                                .header("X-Api-Key", key.key()))
                .andExpect(status().isNotFound());
    }

    @Test
    void contactTagsArePersistedAndBlankValuesRemoveThem() throws Exception {
        CreateApiKeyResponse key = createKey(null);
        UUID contactId = createContactWithTagDefinitions(workspaceId);

        mockMvc.perform(
                        put("/public/v1/contacts/" + contactId + "/tags")
                                .header("X-Api-Key", key.key())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"tier\":\" gold \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactId").value(contactId.toString()))
                .andExpect(jsonPath("$.tags.tier").value("gold"));
        assertThat(persistedTag(contactId)).isEqualTo("gold");

        mockMvc.perform(
                        put("/public/v1/contacts/" + contactId + "/tags")
                                .header("X-Api-Key", key.key())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"tier\":\"  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags").isEmpty());
        assertThat(persistedTag(contactId)).isNull();
    }

    @Test
    void invalidTagValueLeavesPersistedTagsUnchanged() throws Exception {
        CreateApiKeyResponse key = createKey(null);
        UUID contactId = createContactWithTagDefinitions(workspaceId);

        mockMvc.perform(
                        put("/public/v1/contacts/" + contactId + "/tags")
                                .header("X-Api-Key", key.key())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"tier\":\"platinum\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_TAG_VALUE"));
        assertThat(persistedTag(contactId)).isEqualTo("silver");
    }

    @Test
    void unknownTagLeavesPersistedTagsUnchanged() throws Exception {
        CreateApiKeyResponse key = createKey(null);
        UUID contactId = createContactWithTagDefinitions(workspaceId);

        mockMvc.perform(
                        put("/public/v1/contacts/" + contactId + "/tags")
                                .header("X-Api-Key", key.key())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"unknown\":\"gold\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_TAG"));
        assertThat(persistedTag(contactId)).isEqualTo("silver");
    }

    @Test
    void keyCannotUpdateAnotherWorkspacesContactTags() throws Exception {
        CreateApiKeyResponse key = createKey(null);
        UUID contactId = createContactWithTagDefinitions(createWorkspace());

        mockMvc.perform(
                        put("/public/v1/contacts/" + contactId + "/tags")
                                .header("X-Api-Key", key.key())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"tier\":\"gold\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONTACT_NOT_FOUND"));
        assertThat(persistedTag(contactId)).isEqualTo("silver");
    }

    private UUID createContactWithTagDefinitions(UUID workspace) {
        jdbc.update(
                "update workspaces set contact_tag_definitions ="
                        + " '[{\"key\":\"tier\",\"values\":[\"silver\",\"gold\"]}]'::jsonb where id = ?",
                workspace);
        UUID contactId = UUID.randomUUID();
        jdbc.update(
                "insert into contacts (id, workspace_id, tags)"
                        + " values (?, ?, '{\"tier\":\"silver\"}'::jsonb)",
                contactId,
                workspace);
        entityManager.clear();
        return contactId;
    }

    private String persistedTag(UUID contactId) {
        entityManager.flush();
        entityManager.clear();
        return jdbc.queryForObject(
                "select tags ->> 'tier' from contacts where id = ?", String.class, contactId);
    }

    private UUID createWorkspace() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into workspaces (id, name) values (?, 'Integration workspace')", id);
        return id;
    }

    private CreateApiKeyResponse createKey(Instant expiresAt) {
        CreateApiKeyResponse key =
                apiKeyService.createApiKey(
                        workspaceId, userId, new CreateApiKeyRequest("Integration key", expiresAt));
        entityManager.flush();
        entityManager.clear();
        return key;
    }

    private Instant lastUsedAt(UUID keyId) {
        entityManager.flush();
        return jdbc.queryForObject(
                "select last_used_at from workspace_api_keys where id = ?",
                (result, row) -> {
                    java.sql.Timestamp timestamp = result.getTimestamp(1);
                    return timestamp == null ? null : timestamp.toInstant();
                },
                keyId);
    }

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
