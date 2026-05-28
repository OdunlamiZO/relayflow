package com.relayflow.api.telegram;

/**
 * Thrown when an outbound Telegram message cannot be delivered after all retry attempts. Propagates
 * through the {@code @EventListener} back to the originating {@code @Transactional} method so the
 * save is rolled back and the HTTP caller receives an error response.
 */
public class TelegramSendException extends RuntimeException {

    public TelegramSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
