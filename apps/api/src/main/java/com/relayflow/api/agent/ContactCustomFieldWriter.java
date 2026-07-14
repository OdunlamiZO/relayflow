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
 * Persists AI-extracted data onto the contact's custom fields — for keys the workspace has defined
 * as a contact field, or one of the {@link ReservedContactField} keys. An extraction key matching
 * neither stays scoped to the single workflow run ({@code agent.data.*}) rather than polluting the
 * contact.
 *
 * <p>A key is only ever considered if it's also one the admin configured the agent to extract
 * ({@code extractionFields}) — the LLM's {@code extractedData} is free-form model output, and an
 * unconfigured (or hallucinated) key must not get persisted just because it happens to collide with
 * a reserved or workspace-defined field name.
 *
 * <p>Only fills a key that isn't already effectively set — for a reserved key, that includes a
 * value {@link ReservedContactFieldResolver} can already derive (e.g. "displayName" from the
 * contact's display name), not just one explicitly stored in {@code customFields}. Either way, a
 * value already in place is never silently overwritten by a fresh guess.
 */
@Service
public class ContactCustomFieldWriter {

    private final ContactRepository contactRepository;

    private final ExternalIdentityRepository externalIdentityRepository;

    private final ReservedContactFieldResolver reservedContactFieldResolver;

    public ContactCustomFieldWriter(
            ContactRepository contactRepository,
            ExternalIdentityRepository externalIdentityRepository,
            ReservedContactFieldResolver reservedContactFieldResolver) {
        this.contactRepository = contactRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.reservedContactFieldResolver = reservedContactFieldResolver;
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

        // A reserved key can already have a value auto-derived (e.g. "displayName" from the
        // contact's display name, "phone" from a WhatsApp identity) without ever being written to
        // customFields directly. Only resolve derived values — an extra query — when a candidate
        // key could actually need them.
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
        }
    }
}
