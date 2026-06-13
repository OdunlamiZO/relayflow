package com.relayflow.api.telegram;

import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TelegramWebhookRegistrar {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookRegistrar.class);

    private static final String SET_WEBHOOK_URL = "https://api.telegram.org/bot%s/setWebhook";

    private final RestTemplate restTemplate;

    private final String apiBaseUrl;

    public TelegramWebhookRegistrar(
            RestTemplate restTemplate,
            @Value("${relayflow.api.base-url:http://localhost:8080}") String apiBaseUrl) {
        this.restTemplate = restTemplate;
        this.apiBaseUrl = apiBaseUrl;
    }

    /**
     * Registers a per-workspace webhook with Telegram for the given bot token, including a freshly
     * generated {@code secret_token}. Telegram echoes this secret back in the {@code
     * X-Telegram-Bot-Api-Secret-Token} header on every webhook call, allowing the caller to verify
     * requests genuinely originate from Telegram.
     *
     * <p>Returns the generated secret so the caller can persist it. Throws {@link
     * ResponseStatusException} with 502 if Telegram rejects the call, so the caller can surface a
     * meaningful error to the user.
     */
    public String register(String botToken, UUID channelAccountId) {
        String webhookUrl = apiBaseUrl + "/telegram/webhook/" + channelAccountId;
        String url = String.format(SET_WEBHOOK_URL, botToken);
        String secretToken = UUID.randomUUID().toString().replace("-", "");

        try {
            restTemplate.postForObject(
                    url, Map.of("url", webhookUrl, "secret_token", secretToken), String.class);
            log.info(
                    "Registered Telegram webhook: channelAccountId={}, url={}",
                    channelAccountId,
                    webhookUrl);

            return secretToken;
        } catch (Exception e) {
            log.error(
                    "Failed to register Telegram webhook for channel account {}: {}",
                    channelAccountId,
                    e.getMessage());

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not register webhook with Telegram. Check your bot token and try again.");
        }
    }
}
