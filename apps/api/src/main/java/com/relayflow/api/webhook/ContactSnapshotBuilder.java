package com.relayflow.api.webhook;

import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.webhook.dto.ContactSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ContactSnapshotBuilder {

    public static ContactSnapshot build(Contact contact) {
        Map<String, Object> contactData = new LinkedHashMap<>();
        contactData.put("id", contact.getId().toString());
        contactData.put(
                "displayName", contact.getDisplayName() != null ? contact.getDisplayName() : "");
        contactData.putAll(contact.getCustomFields());
        contactData.put("tags", Map.copyOf(contact.getTags()));

        return new ContactSnapshot(contactData);
    }

    private ContactSnapshotBuilder() {}
}
