package io.odunlamizo.relayflow.agent;

import io.odunlamizo.relayflow.agent.domain.ExtractionField;
import io.odunlamizo.relayflow.contact.ContactDisplayNameSync;
import io.odunlamizo.relayflow.contact.domain.Contact;
import io.odunlamizo.relayflow.contact.repository.ContactRepository;
import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.webhook.ContactSnapshotBuilder;
import io.odunlamizo.relayflow.webhook.WebhookDispatchService;
import io.odunlamizo.relayflow.webhook.domain.WebhookEventType;
import io.odunlamizo.relayflow.workspace.domain.ContactFieldDefinition;
import io.odunlamizo.relayflow.workspace.domain.ReservedContactField;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Writes configured extraction fields that are writable and not already stored. */
@Service
public class ContactCustomFieldWriter {

    private final ContactRepository contactRepository;

    private final WebhookDispatchService webhookDispatchService;

    public ContactCustomFieldWriter(
            ContactRepository contactRepository, WebhookDispatchService webhookDispatchService) {
        this.contactRepository = contactRepository;
        this.webhookDispatchService = webhookDispatchService;
    }

    public void apply(
            Conversation conversation,
            Map<String, String> extractedData,
            List<ExtractionField> extractionFields) {
        if (extractedData.isEmpty()) {
            return;
        }

        Set<String> knownExtractionKeys =
                extractionFields.stream().map(ExtractionField::key).collect(Collectors.toSet());

        Set<String> writableKeys =
                Arrays.stream(ReservedContactField.values())
                        .map(ReservedContactField::key)
                        .collect(Collectors.toCollection(HashSet::new));
        writableKeys.addAll(
                conversation.getWorkspace().getContactFieldDefinitions().stream()
                        .map(ContactFieldDefinition::key)
                        .collect(Collectors.toSet()));

        Set<String> candidateKeys = new LinkedHashSet<>(extractedData.keySet());
        candidateKeys.retainAll(knownExtractionKeys);
        candidateKeys.retainAll(writableKeys);

        if (candidateKeys.isEmpty()) {
            return;
        }

        Contact contact = conversation.getContact();
        Map<String, String> customFields = contact.getCustomFields();

        boolean changed = false;

        for (String key : candidateKeys) {
            String incoming = extractedData.get(key);

            if (incoming == null || incoming.isBlank()) {
                continue;
            }

            String existing = customFields.get(key);
            boolean alreadyStored = existing != null && !existing.isBlank();

            if (!alreadyStored) {
                customFields.put(key, incoming);
                changed = true;
            }
        }

        if (changed) {
            ContactDisplayNameSync.apply(contact);
            contactRepository.save(contact);

            webhookDispatchService.dispatch(
                    conversation.getWorkspace().getId(),
                    WebhookEventType.CONTACT_UPDATED,
                    ContactSnapshotBuilder.build(contact));
        }
    }
}
