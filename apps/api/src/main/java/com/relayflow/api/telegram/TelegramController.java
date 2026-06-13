package com.relayflow.api.telegram;

import com.relayflow.api.telegram.dto.TelegramWebhookPayload;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/telegram")
public class TelegramController {

    private final TelegramAdapter telegramAdapter;

    public TelegramController(TelegramAdapter telegramAdapter) {
        this.telegramAdapter = telegramAdapter;
    }

    /**
     * Telegram calls this URL for every update when a webhook is registered. The URL to register
     * with Telegram is: https://{your-domain}/telegram/webhook/{channelAccountId}
     *
     * <p>Register it via the Telegram Bot API: POST https://api.telegram.org/bot{token}/setWebhook
     * Body: { "url": "https://{your-domain}/telegram/webhook/{channelAccountId}" }
     */
    @PostMapping("/webhook/{channelAccountId}")
    @ResponseStatus(HttpStatus.OK)
    void webhook(
            @PathVariable UUID channelAccountId,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false)
                    String secretToken,
            @RequestBody TelegramWebhookPayload payload) {
        telegramAdapter.handleWebhook(channelAccountId, secretToken, payload);
    }

    /**
     * Single webhook endpoint for the shared bot. Routes messages to the correct guest workspace
     * via the /start {workspaceId} deep link.
     */
    @PostMapping("/webhook/shared")
    @ResponseStatus(HttpStatus.OK)
    void sharedBotWebhook(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false)
                    String secretToken,
            @RequestBody TelegramWebhookPayload payload) {
        telegramAdapter.handleSharedBotWebhook(secretToken, payload);
    }
}
