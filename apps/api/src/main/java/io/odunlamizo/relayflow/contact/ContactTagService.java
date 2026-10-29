package io.odunlamizo.relayflow.contact;

import io.odunlamizo.relayflow.contact.domain.Contact;
import io.odunlamizo.relayflow.contact.repository.ContactRepository;
import io.odunlamizo.relayflow.webhook.ContactSnapshotBuilder;
import io.odunlamizo.relayflow.webhook.WebhookDispatchService;
import io.odunlamizo.relayflow.webhook.domain.WebhookEventType;
import io.odunlamizo.relayflow.workspace.domain.ContactTagDefinition;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A blank value clears the tag. */
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
                throw new UnknownContactTagException(key);
            }

            String value = change.getValue() != null ? change.getValue().trim() : "";

            if (value.isEmpty()) {
                tags.remove(key);

                continue;
            }

            if (!definition.values().contains(value)) {
                throw new InvalidContactTagValueException(key, value, definition.values());
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
