package io.odunlamizo.relayflow.telegram;

import static net.logstash.logback.argument.StructuredArguments.kv;

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

    public String register(String botToken, UUID channelAccountId) {
        String webhookUrl = apiBaseUrl + "/telegram/webhook/" + channelAccountId;
        String url = String.format(SET_WEBHOOK_URL, botToken);
        String secretToken = UUID.randomUUID().toString().replace("-", "");

        try {
            restTemplate.postForObject(
                    url, Map.of("url", webhookUrl, "secret_token", secretToken), String.class);
            log.info(
                    "Telegram webhook registered {} {}",
                    kv("channelAccountId", channelAccountId),
                    kv("webhookUrl", webhookUrl));

            return secretToken;
        } catch (Exception exception) {
            log.error(
                    "Telegram webhook registration failed {} {}",
                    kv("channelAccountId", channelAccountId),
                    kv("error", TelegramFailures.describe(exception)));

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not register webhook with Telegram. Check your bot token and try again.");
        }
    }
}
