package io.odunlamizo.relayflow.telegram;

// Docs: docs/codebase-components.md#telegramcontroller

import io.odunlamizo.relayflow.telegram.dto.TelegramWebhookPayload;
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

    @PostMapping("/webhook/{channelAccountId}")
    @ResponseStatus(HttpStatus.OK)
    void webhook(
            @PathVariable UUID channelAccountId,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false)
                    String secretToken,
            @RequestBody TelegramWebhookPayload payload) {
        telegramAdapter.handleWebhook(channelAccountId, secretToken, payload);
    }
}
