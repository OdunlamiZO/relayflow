package io.odunlamizo.relayflow.contact;

// Docs: docs/codebase-components.md#contact-exceptions

import io.odunlamizo.relayflow.common.ResourceNotFoundException;

public class ContactNotFoundException extends ResourceNotFoundException {

    public ContactNotFoundException() {
        super("Contact not found");
    }
}
