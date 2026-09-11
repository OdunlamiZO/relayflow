package com.relayflow.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.messaging.domain.Workspace;
import com.relayflow.api.messaging.domain.WorkspaceMember;
import com.relayflow.api.messaging.domain.WorkspaceRole;
import com.relayflow.api.messaging.dto.CreateContactRequest;
import com.relayflow.api.messaging.repository.WorkspaceMemberRepository;
import com.relayflow.api.messaging.repository.WorkspaceRepository;
import com.relayflow.api.webhook.WebhookUrlValidator;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Exercises a full messaging-to-webhook flow through real HTTP requests, real Postgres, and a real
 * (async) HTTP delivery to a local WireMock stub: create a contact, edit its custom fields, and
 * confirm the resulting {@code contact.updated} webhook is delivered with a valid HMAC signature
 * and the expected payload.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(ContactWebhookDispatchIT.PermissiveWebhookUrlValidatorConfiguration.class)
class ContactWebhookDispatchIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;

    private WireMockServer wireMockServer;
    private String userEmail;
    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        wireMockServer.stubFor(
                WireMock.post(WireMock.urlEqualTo("/webhook-received"))
                        .willReturn(WireMock.aResponse().withStatus(200)));

        userEmail = "owner-" + UUID.randomUUID() + "@example.com";

        User user = new User();
        user.setEmail(userEmail);
        user.setDisplayName("Workspace Owner");
        user = userRepository.save(user);

        Workspace workspace = new Workspace();
        workspace.setName("Acme Support");
        workspace = workspaceRepository.save(workspace);
        workspaceId = workspace.getId();

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspaceId);
        member.setUserId(user.getId());
        member.setRole(WorkspaceRole.OWNER);
        workspaceMemberRepository.save(member);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void contactCustomFieldUpdateDispatchesSignedWebhook() throws Exception {
        String webhookUrl = wireMockServer.baseUrl() + "/webhook-received";

        String saveWebhookBody =
                """
                {"url": "%s", "enabled": true, "events": ["CONTACT_UPDATED"]}
                """
                        .formatted(webhookUrl);

        String saveWebhookResponse =
                mockMvc.perform(
                                put("/workspaces/{workspaceId}/webhook", workspaceId)
                                        .with(user(userEmail))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(saveWebhookBody))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String webhookSecret =
                objectMapper.readTree(saveWebhookResponse).get("generatedSecret").asText();

        String createContactBody =
                objectMapper.writeValueAsString(
                        new CreateContactRequest(workspaceId, "Ada Lovelace"));

        String createContactResponse =
                mockMvc.perform(
                                post("/contacts")
                                        .with(user(userEmail))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(createContactBody))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        UUID contactId =
                UUID.fromString(objectMapper.readTree(createContactResponse).get("id").asText());

        mockMvc.perform(
                        patch("/contacts/{id}/custom-fields", contactId)
                                .queryParam("workspaceId", workspaceId.toString())
                                .with(user(userEmail))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"customFields": {"orderNumber": "12345"}}
                                        """))
                .andExpect(status().isOk());

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () ->
                                wireMockServer.verify(
                                        1,
                                        WireMock.postRequestedFor(
                                                WireMock.urlEqualTo("/webhook-received"))));

        var delivered =
                wireMockServer
                        .findAll(
                                WireMock.postRequestedFor(WireMock.urlEqualTo("/webhook-received")))
                        .getFirst();
        String rawBody = delivered.getBodyAsString();
        JsonNode payload = objectMapper.readTree(rawBody);

        assertThat(payload.get("event").asText()).isEqualTo("contact.updated");
        assertThat(payload.get("workspaceId").asText()).isEqualTo(workspaceId.toString());
        assertThat(payload.get("data").get("contact").get("orderNumber").asText())
                .isEqualTo("12345");
        assertThat(payload.get("data").get("contact").get("id").asText())
                .isEqualTo(contactId.toString());

        String expectedSignature = "sha256=" + hmacSha256Hex(webhookSecret, rawBody);
        String actualSignature = delivered.getHeader("X-RelayFlow-Signature");

        assertThat(actualSignature).isEqualTo(expectedSignature);
    }

    private static String hmacSha256Hex(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));

        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes()));
    }

    @TestConfiguration
    static class PermissiveWebhookUrlValidatorConfiguration {

        /**
         * The production {@link WebhookUrlValidator} rejects loopback addresses (SSRF protection),
         * which would otherwise reject this test's local WireMock URL. Overridden for this test
         * only — the real validator has its own unit test.
         */
        @Bean
        @Primary
        WebhookUrlValidator permissiveWebhookUrlValidator() {
            return new WebhookUrlValidator() {
                @Override
                public void validate(String url) {}

                @Override
                public boolean isSafe(String url) {
                    return true;
                }
            };
        }
    }
}
