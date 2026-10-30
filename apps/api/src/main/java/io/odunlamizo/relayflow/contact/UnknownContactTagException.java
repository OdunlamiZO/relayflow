package io.odunlamizo.relayflow.contact;

// Docs: docs/codebase-components.md#contact-exceptions

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UnknownContactTagException extends ResponseStatusException {

    public UnknownContactTagException(String key) {
        super(HttpStatus.BAD_REQUEST, "\"" + key + "\" is not a contact tag.");
    }
}
