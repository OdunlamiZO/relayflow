package com.relayflow.api.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.security.CredentialEncryptionService;
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

/**
 * Delivers webhook events to workspace-configured endpoints.
 *
 * <p>Each delivery runs on the {@code webhookExecutor} thread pool so it never blocks the main
 * request thread. Failed deliveries are retried up to 3 times with exponential backoff (1 s → 5 s →
 * 30 s).
 */
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
        webhookRepository
                .findByWorkspace(workspaceId)
                .filter(WorkspaceWebhook::isEnabled)
                .filter(webhook -> webhook.getEvents().contains(eventType))
                .ifPresent(webhook -> deliver(webhook, eventType, data));
    }

    private void deliver(WorkspaceWebhook webhook, WebhookEventType eventType, Object data) {
        if (!urlValidator.isSafe(webhook.getUrl())) {
            log.error(
                    "Webhook delivery skipped — URL no longer resolves to a public address:"
                            + " event={}, workspace={}",
                    eventType,
                    webhook.getWorkspaceId());

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
        } catch (Exception e) {
            log.error("Failed to serialise webhook payload for event={}", eventType, e);

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
                            "Webhook delivered: event={}, workspace={}, status={}",
                            eventType,
                            webhook.getWorkspaceId(),
                            response.statusCode());

                    return;
                }

                log.warn(
                        "Webhook rejected: event={}, workspace={}, status={}, attempt={}/{}",
                        eventType,
                        webhook.getWorkspaceId(),
                        response.statusCode(),
                        attempt + 1,
                        RETRY_DELAYS_SECONDS.length + 1);
            } catch (Exception e) {
                log.warn(
                        "Webhook delivery failed: event={}, workspace={}, attempt={}/{}: {}",
                        eventType,
                        webhook.getWorkspaceId(),
                        attempt + 1,
                        RETRY_DELAYS_SECONDS.length + 1,
                        e.getMessage());
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
                "Webhook delivery exhausted all retries: event={}, workspace={}",
                eventType,
                webhook.getWorkspaceId());
    }

    private static String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign webhook payload", e);
        }
    }
}
