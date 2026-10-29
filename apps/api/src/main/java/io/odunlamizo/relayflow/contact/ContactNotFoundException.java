package io.odunlamizo.relayflow.contact;

import io.odunlamizo.relayflow.common.ResourceNotFoundException;

public class ContactNotFoundException extends ResourceNotFoundException {

    public ContactNotFoundException() {
        super("Contact not found");
    }
}
