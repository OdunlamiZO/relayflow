package com.relayflow.api.telegram;

import org.springframework.web.client.RestClientResponseException;

/** Describes a Telegram API failure without the request URL, which embeds the bot token. */
final class TelegramFailures {

    private TelegramFailures() {}

    static String describe(Exception exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return "HTTP "
                    + responseException.getStatusCode().value()
                    + ": "
                    + responseException.getResponseBodyAsString();
        }

        return exception.getClass().getSimpleName();
    }
}
