package com.relayflow.api.whatsapp;

/**
 * Thrown when an outbound WhatsApp message cannot be delivered after all retry attempts. Propagates
 * through the {@code @EventListener} back to the originating {@code @Transactional} method so the
 * save is rolled back and the HTTP caller receives an error response.
 */
public class WhatsAppSendException extends RuntimeException {

    public WhatsAppSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
