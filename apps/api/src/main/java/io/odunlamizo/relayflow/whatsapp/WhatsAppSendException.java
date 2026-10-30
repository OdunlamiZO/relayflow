package io.odunlamizo.relayflow.whatsapp;

// Docs: docs/codebase-components.md#whatsappsendexception

public class WhatsAppSendException extends RuntimeException {

    public WhatsAppSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
