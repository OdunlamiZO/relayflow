package io.odunlamizo.relayflow.webhook;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.security.CredentialEncryptionService;
import io.odunlamizo.relayflow.webhook.domain.WebhookEventType;
import io.odunlamizo.relayflow.webhook.domain.WorkspaceWebhook;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class WebhookDispatchService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatchService.class);

    private static final String SIGNATURE_HEADER = "X-RelayFlow-Signature";

    private static final int[] RETRY_DELAYS_SECONDS = {1, 5, 30};

    private final WorkspaceWebhookRepository webhookRepository;

    private final CredentialEncryptionService encryptionService;

    private final WebhookUrlValidator urlValidator;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    private final Duration httpTimeout;

    public WebhookDispatchService(
            WorkspaceWebhookRepository webhookRepository,
            CredentialEncryptionService encryptionService,
            WebhookUrlValidator urlValidator,
            ObjectMapper objectMapper,
            @Value("${relayflow.webhook.timeout-ms}") long timeoutMs) {
        this.webhookRepository = webhookRepository;
        this.encryptionService = encryptionService;
        this.urlValidator = urlValidator;
        this.objectMapper = objectMapper;
        this.httpTimeout = Duration.ofMillis(timeoutMs);
        this.httpClient = HttpClient.newBuilder().connectTimeout(httpTimeout).build();
    }

    @Async("webhookExecutor")
    public void dispatch(UUID workspaceId, WebhookEventType eventType, Object data) {
        webhookRepository.findByWorkspace(workspaceId).stream()
                .filter(WorkspaceWebhook::isEnabled)
                .filter(webhook -> webhook.getEvents().contains(eventType))
                .forEach(webhook -> deliver(webhook, eventType, data));
    }

    private void deliver(WorkspaceWebhook webhook, WebhookEventType eventType, Object data) {
        if (!urlValidator.isSafe(webhook.getUrl())) {
            log.warn(
                    "Webhook delivery skipped, URL no longer resolves to a public address {} {} {}",
                    kv("workspaceId", webhook.getWorkspaceId()),
                    kv("webhookId", webhook.getId()),
                    kv("eventType", eventType));

            return;
        }

        String payload;

        try {
            payload =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "event", eventType.getEventName(),
                                    "timestamp", Instant.now().toString(),
                                    "workspaceId", webhook.getWorkspaceId().toString(),
                                    "data", data));
        } catch (Exception exception) {
            log.error(
                    "Webhook payload serialization failed {} {} {}",
                    kv("workspaceId", webhook.getWorkspaceId()),
                    kv("webhookId", webhook.getId()),
                    kv("eventType", eventType),
                    exception);

            return;
        }

        String secret = encryptionService.decrypt(webhook.getSecret());
        String signature = sign(secret, payload);

        for (int attempt = 0; attempt <= RETRY_DELAYS_SECONDS.length; attempt++) {
            try {
                HttpRequest request =
                        HttpRequest.newBuilder()
                                .uri(URI.create(webhook.getUrl()))
                                .timeout(httpTimeout)
                                .header("Content-Type", "application/json")
                                .header(SIGNATURE_HEADER, "sha256=" + signature)
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                payload, StandardCharsets.UTF_8))
                                .build();

                HttpResponse<Void> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.discarding());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.debug(
                            "Webhook delivered {} {} {} {}",
                            kv("workspaceId", webhook.getWorkspaceId()),
                            kv("webhookId", webhook.getId()),
                            kv("eventType", eventType),
                            kv("statusCode", response.statusCode()));

                    return;
                }

                log.warn(
                        "Webhook delivery rejected {} {} {} {} {} {}",
                        kv("workspaceId", webhook.getWorkspaceId()),
                        kv("webhookId", webhook.getId()),
                        kv("eventType", eventType),
                        kv("statusCode", response.statusCode()),
                        kv("attempt", attempt + 1),
                        kv("maxAttempts", RETRY_DELAYS_SECONDS.length + 1));
            } catch (Exception exception) {
                log.warn(
                        "Webhook delivery failed {} {} {} {} {} {}",
                        kv("workspaceId", webhook.getWorkspaceId()),
                        kv("webhookId", webhook.getId()),
                        kv("eventType", eventType),
                        kv("attempt", attempt + 1),
                        kv("maxAttempts", RETRY_DELAYS_SECONDS.length + 1),
                        kv("error", String.valueOf(exception)));
            }

            if (attempt < RETRY_DELAYS_SECONDS.length) {
                try {
                    Thread.sleep(Duration.ofSeconds(RETRY_DELAYS_SECONDS[attempt]));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();

                    return;
                }
            }
        }

        log.error(
                "Webhook delivery retries exhausted {} {} {}",
                kv("workspaceId", webhook.getWorkspaceId()),
                kv("webhookId", webhook.getId()),
                kv("eventType", eventType));
    }

    private static String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hmac);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to sign webhook payload", exception);
        }
    }
}
