package io.odunlamizo.relayflow.contact;

// Docs: docs/codebase-components.md#contactdisplaynamesync

import io.odunlamizo.relayflow.contact.domain.Contact;
import io.odunlamizo.relayflow.workspace.domain.ReservedContactField;

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
