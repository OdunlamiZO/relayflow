package io.odunlamizo.relayflow.telegram;

// Docs: docs/codebase-components.md#telegramsendexception

public class TelegramSendException extends RuntimeException {

    public TelegramSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
