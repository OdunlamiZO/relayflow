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
     * Registers a per-workspace webhook with Telegram for the given bot token. Throws {@link
     * ResponseStatusException} with 502 if Telegram rejects the call, so the caller can surface a
     * meaningful error to the user.
     */
    public void register(String botToken, UUID channelAccountId) {
        String webhookUrl = apiBaseUrl + "/api/telegram/webhook/" + channelAccountId;
        String url = String.format(SET_WEBHOOK_URL, botToken);

        try {
            restTemplate.postForObject(url, Map.of("url", webhookUrl), String.class);
            log.info(
                    "Registered Telegram webhook: channelAccountId={}, url={}",
                    channelAccountId,
                    webhookUrl);
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
