package com.relayflow.api.whatsapp;

public class WhatsAppSendException extends RuntimeException {

    public WhatsAppSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
