package io.odunlamizo.relayflow.telegram;

public class TelegramSendException extends RuntimeException {

    public TelegramSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
