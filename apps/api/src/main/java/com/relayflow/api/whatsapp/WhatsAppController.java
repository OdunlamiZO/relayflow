package com.relayflow.api.whatsapp;

import com.relayflow.api.whatsapp.dto.WhatsAppWebhookPayload;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private final WhatsAppAdapter whatsAppAdapter;

    public WhatsAppController(WhatsAppAdapter whatsAppAdapter) {
        this.whatsAppAdapter = whatsAppAdapter;
    }

    /**
     * Meta calls this URL (GET) when you register or update a webhook in the Developer Console.
     * Responds with {@code hub.challenge} as plain text on a token match; 403 otherwise.
     *
     * <p>Register this URL in Meta: https://{your-domain}/api/whatsapp/webhook/{channelAccountId}
     */
    @GetMapping(value = "/webhook/{channelAccountId}", produces = MediaType.TEXT_PLAIN_VALUE)
    String verifyWebhook(
            @PathVariable UUID channelAccountId,
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {
        return whatsAppAdapter.verifyWebhook(channelAccountId, mode, verifyToken, challenge);
    }

    /**
     * Meta calls this URL (POST) for every inbound message event. Returns 200 immediately;
     * processing is synchronous within the request.
     */
    @PostMapping("/webhook/{channelAccountId}")
    @ResponseStatus(HttpStatus.OK)
    void webhook(@PathVariable UUID channelAccountId, @RequestBody WhatsAppWebhookPayload payload) {
        whatsAppAdapter.handleWebhook(channelAccountId, payload);
    }
}
