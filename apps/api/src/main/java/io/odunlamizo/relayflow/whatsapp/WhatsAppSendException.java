package io.odunlamizo.relayflow.whatsapp;

public class WhatsAppSendException extends RuntimeException {

    public WhatsAppSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
