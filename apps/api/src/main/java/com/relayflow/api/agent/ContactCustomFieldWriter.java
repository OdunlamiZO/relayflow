package com.relayflow.api.agent;

import com.relayflow.api.agent.domain.ExtractionField;
import com.relayflow.api.messaging.ReservedContactFieldResolver;
import com.relayflow.api.messaging.domain.Contact;
import com.relayflow.api.messaging.domain.ContactFieldDefinition;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.ExternalIdentity;
import com.relayflow.api.messaging.domain.ReservedContactField;
import com.relayflow.api.messaging.repository.ContactRepository;
import com.relayflow.api.messaging.repository.ExternalIdentityRepository;
import com.relayflow.api.webhook.ContactSnapshotBuilder;
import com.relayflow.api.webhook.WebhookDispatchService;
import com.relayflow.api.webhook.domain.WebhookEventType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Persists AI-extracted data onto a contact's custom fields — only a key that is both a configured
 * {@code extractionField} and a writable contact field ({@link ReservedContactField} or a
 * workspace-defined {@link ContactFieldDefinition}), and only when not already effectively set
 * (including a value {@link ReservedContactFieldResolver} can derive).
 */
@Service
public class ContactCustomFieldWriter {

    private final ContactRepository contactRepository;

    private final ExternalIdentityRepository externalIdentityRepository;

    private final ReservedContactFieldResolver reservedContactFieldResolver;

    private final WebhookDispatchService webhookDispatchService;

    public ContactCustomFieldWriter(
            ContactRepository contactRepository,
            ExternalIdentityRepository externalIdentityRepository,
            ReservedContactFieldResolver reservedContactFieldResolver,
            WebhookDispatchService webhookDispatchService) {
        this.contactRepository = contactRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.reservedContactFieldResolver = reservedContactFieldResolver;
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

        Map<String, String> effectiveValues = customFields;
        if (candidateKeys.stream().anyMatch(ReservedContactField::isReserved)) {
            List<ExternalIdentity> identities =
                    externalIdentityRepository.findByContact(contact.getId());
            effectiveValues =
                    new LinkedHashMap<>(reservedContactFieldResolver.resolve(contact, identities));
            effectiveValues.putAll(customFields);
        }

        boolean changed = false;

        for (String key : candidateKeys) {
            String existing = effectiveValues.get(key);
            boolean alreadyConfigured = existing != null && !existing.isBlank();

            if (!alreadyConfigured) {
                customFields.put(key, extractedData.get(key));
                changed = true;
            }
        }

        if (changed) {
            contactRepository.save(contact);

            webhookDispatchService.dispatch(
                    conversation.getWorkspace().getId(),
                    WebhookEventType.CONTACT_UPDATED,
                    ContactSnapshotBuilder.build(contact));
        }
    }
}
