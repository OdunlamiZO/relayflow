package com.relayflow.api.contact;

import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.contact.repository.ContactRepository;
import com.relayflow.api.webhook.ContactSnapshotBuilder;
import com.relayflow.api.webhook.WebhookDispatchService;
import com.relayflow.api.webhook.domain.WebhookEventType;
import com.relayflow.api.workspace.domain.ContactTagDefinition;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Sets or clears a contact's tags; a blank value clears the tag. */
@Service
public class ContactTagService {

    private final ContactRepository contactRepository;

    private final WebhookDispatchService webhookDispatchService;

    public ContactTagService(
            ContactRepository contactRepository, WebhookDispatchService webhookDispatchService) {
        this.contactRepository = contactRepository;
        this.webhookDispatchService = webhookDispatchService;
    }

    @Transactional
    public Contact applyTags(Contact contact, Map<String, String> changes) {
        Map<String, ContactTagDefinition> definitions =
                contact.getWorkspace().getContactTagDefinitions().stream()
                        .collect(Collectors.toMap(ContactTagDefinition::key, Function.identity()));

        Map<String, String> tags = new LinkedHashMap<>(contact.getTags());

        for (Map.Entry<String, String> change : changes.entrySet()) {
            String key = change.getKey();
            ContactTagDefinition definition = definitions.get(key);

            if (definition == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "\"" + key + "\" is not a contact tag.");
            }

            String value = change.getValue() != null ? change.getValue().trim() : "";

            if (value.isEmpty()) {
                tags.remove(key);

                continue;
            }

            if (!definition.values().contains(value)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "\""
                                + value
                                + "\" is not an allowed value for \""
                                + key
                                + "\". Allowed values: "
                                + String.join(", ", definition.values())
                                + ".");
            }

            tags.put(key, value);
        }

        if (!tags.equals(contact.getTags())) {
            contact.setTags(tags);
            contactRepository.save(contact);

            webhookDispatchService.dispatch(
                    contact.getWorkspace().getId(),
                    WebhookEventType.CONTACT_UPDATED,
                    ContactSnapshotBuilder.build(contact));
        }

        return contact;
    }
}
