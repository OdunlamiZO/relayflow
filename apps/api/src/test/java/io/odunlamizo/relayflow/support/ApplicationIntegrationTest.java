package io.odunlamizo.relayflow.support;

// Docs: docs/codebase-components.md#backend-tests

import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

/** Real services, migrations and PostgreSQL; each test rolls its data back. */
@SpringBootTest(properties = "relayflow.ratelimit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApplicationIntegrationTest.DatabaseConfiguration.class)
@Transactional
public abstract class ApplicationIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    public static class DatabaseConfiguration {
        // Spring owns the container, so it lives as long as the cached application context.
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:16-alpine");
        }
    }

    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected EntityManager entityManager;

    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    protected UUID workspace() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into workspaces (id, name) values (?, 'Integration workspace')", id);
        return id;
    }

    protected UUID user() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into users (id, email) values (?, ?)", id, id + "@example.com");
        return id;
    }

    protected UUID contact(UUID workspaceId) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into contacts (id, workspace_id) values (?, ?)", id, workspaceId);
        return id;
    }

    protected UUID channel(UUID workspaceId) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into channel_accounts (id, workspace_id, provider, name, status)"
                        + " values (?, ?, 'TELEGRAM', 'Integration bot', 'ACTIVE')",
                id,
                workspaceId);
        return id;
    }

    protected UUID conversation(UUID workspaceId, UUID contactId, UUID channelId) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into conversations (id, workspace_id, contact_id, channel_account_id, status)"
                        + " values (?, ?, ?, ?, 'OPEN')",
                id,
                workspaceId,
                contactId,
                channelId);
        return id;
    }

    protected UUID message(UUID workspaceId, UUID conversationId, String text, Instant createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into messages"
                        + " (id, workspace_id, conversation_id, direction, sender_type, text, created_at)"
                        + " values (?, ?, ?, 'INBOUND', 'CONTACT', ?, ?)",
                id,
                workspaceId,
                conversationId,
                text,
                Timestamp.from(createdAt));
        return id;
    }
}
