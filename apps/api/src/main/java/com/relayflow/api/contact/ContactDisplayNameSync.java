package com.relayflow.api.contact;

import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.workspace.domain.ReservedContactField;

/** Keeps {@code displayName} in sync with firstName/lastName once either is known. */
public final class ContactDisplayNameSync {

    public static void apply(Contact contact) {
        String firstName =
                contact.getCustomFields().getOrDefault(ReservedContactField.FIRST_NAME.key(), "");
        String lastName =
                contact.getCustomFields().getOrDefault(ReservedContactField.LAST_NAME.key(), "");
        String fullName = (firstName + " " + lastName).trim();

        if (!fullName.isBlank()) {
            contact.setDisplayName(fullName);
        }
    }

    private ContactDisplayNameSync() {}
}
