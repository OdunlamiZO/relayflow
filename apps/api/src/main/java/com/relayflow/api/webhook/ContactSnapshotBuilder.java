package com.relayflow.api.webhook;

import com.relayflow.api.messaging.domain.Contact;
import com.relayflow.api.webhook.dto.ContactSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds {@link ContactSnapshot}, shared by every write path (manual edit, AI extraction, workflow
 * Set Contact Field node) so the shape can't drift between them.
 */
public final class ContactSnapshotBuilder {

    public static ContactSnapshot build(Contact contact) {
        Map<String, Object> contactData = new LinkedHashMap<>();
        contactData.put("id", contact.getId().toString());
        contactData.put(
                "displayName", contact.getDisplayName() != null ? contact.getDisplayName() : "");
        contactData.putAll(contact.getCustomFields());

        return new ContactSnapshot(contactData);
    }

    private ContactSnapshotBuilder() {}
}
