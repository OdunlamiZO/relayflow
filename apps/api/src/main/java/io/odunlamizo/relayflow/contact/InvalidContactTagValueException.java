package io.odunlamizo.relayflow.contact;

// Docs: docs/codebase-components.md#contact-exceptions

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidContactTagValueException extends ResponseStatusException {

    public InvalidContactTagValueException(String key, String value, List<String> allowedValues) {
        super(
                HttpStatus.BAD_REQUEST,
                "\""
                        + value
                        + "\" is not an allowed value for \""
                        + key
                        + "\". Allowed values: "
                        + String.join(", ", allowedValues)
                        + ".");
    }
}
