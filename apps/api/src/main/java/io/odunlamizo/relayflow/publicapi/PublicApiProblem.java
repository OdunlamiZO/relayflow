package io.odunlamizo.relayflow.publicapi;

// Docs: docs/codebase-components.md#publicapiproblem

import java.util.Locale;
import org.springframework.http.HttpStatus;

/** Every error the public API returns. The name is the {@code code} field clients branch on. */
public enum PublicApiProblem {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    UNKNOWN_TAG(HttpStatus.BAD_REQUEST, "Unknown tag"),
    INVALID_TAG_VALUE(HttpStatus.BAD_REQUEST, "Invalid tag value"),
    MISSING_API_KEY(HttpStatus.UNAUTHORIZED, "Missing API key"),
    INVALID_API_KEY(HttpStatus.UNAUTHORIZED, "Invalid API key"),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "Endpoint not found"),
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Conversation not found"),
    CONTACT_NOT_FOUND(HttpStatus.NOT_FOUND, "Contact not found"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed"),
    CONVERSATION_LOCKED(HttpStatus.CONFLICT, "Conversation locked"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Rate limited"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error"),
    CHANNEL_DELIVERY_FAILED(HttpStatus.BAD_GATEWAY, "Channel delivery failed");

    private final HttpStatus status;

    private final String title;

    PublicApiProblem(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    /** A relative URI such as {@code /problems/conversation-not-found}. */
    public String type() {
        return "/problems/" + name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
