package com.relayflow.api.agent;

import com.relayflow.api.agent.domain.ExtractionField;
import com.relayflow.api.contact.ContactDisplayNameSync;
import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.contact.repository.ContactRepository;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.webhook.ContactSnapshotBuilder;
import com.relayflow.api.webhook.WebhookDispatchService;
import com.relayflow.api.webhook.domain.WebhookEventType;
import com.relayflow.api.workspace.domain.ContactFieldDefinition;
import com.relayflow.api.workspace.domain.ReservedContactField;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Persists AI-extracted data onto a contact's custom fields — only a key that is both a configured
 * {@code extractionField} and a writable contact field ({@link ReservedContactField} or a
 * workspace-defined {@link ContactFieldDefinition}), and only when not already explicitly stored. A
 * reserved key that {@link com.relayflow.api.contact.ReservedContactFieldResolver} could derive but
 * hasn't yet been stored is still written — the point is to record that the AI captured it, and to
 * fire {@code contact.updated}.
 */
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
