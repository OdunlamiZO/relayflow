package com.relayflow.api.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.relayflow.api.agent.domain.ExtractionField;
import com.relayflow.api.contact.ReservedContactFieldResolver;
import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.contact.repository.ContactRepository;
import com.relayflow.api.contact.repository.ExternalIdentityRepository;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.webhook.WebhookDispatchService;
import com.relayflow.api.workspace.domain.ContactFieldDefinition;
import com.relayflow.api.workspace.domain.Workspace;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactCustomFieldWriterTest {

    @Mock private ContactRepository contactRepository;

    @Mock private ExternalIdentityRepository externalIdentityRepository;

    @Mock private ReservedContactFieldResolver reservedContactFieldResolver;

    @Mock private WebhookDispatchService webhookDispatchService;

    private ContactCustomFieldWriter writer() {
        return new ContactCustomFieldWriter(
                contactRepository,
                externalIdentityRepository,
                reservedContactFieldResolver,
                webhookDispatchService);
    }

    private Conversation conversationWithDefinedFields(String... keys) {
        Workspace workspace = new Workspace();
        workspace.setContactFieldDefinitions(
                List.of(keys).stream()
                        .map(key -> new ContactFieldDefinition(key, key, ""))
                        .toList());

        Contact contact = new Contact();
        contact.setId(UUID.randomUUID());

        Conversation conversation = new Conversation();
        conversation.setWorkspace(workspace);
        conversation.setContact(contact);

        return conversation;
    }

    private List<ExtractionField> extractionFields(String... keys) {
        return List.of(keys).stream().map(key -> new ExtractionField(key, "")).toList();
    }

    @Test
    void writesOnlyKeysThatMatchADefinedContactField() {
        Conversation conversation = conversationWithDefinedFields("orderNumber");

        writer().apply(
                        conversation,
                        Map.of("orderNumber", "12345", "sentiment", "happy"),
                        extractionFields("orderNumber", "sentiment"));

        assertThat(conversation.getContact().getCustomFields())
                .containsExactly(Map.entry("orderNumber", "12345"));
        verify(contactRepository).save(conversation.getContact());
    }

    @Test
    void doesNothingWhenNoExtractedKeyMatchesADefinition() {
        Conversation conversation = conversationWithDefinedFields("orderNumber");

        writer().apply(conversation, Map.of("sentiment", "happy"), extractionFields("sentiment"));

        assertThat(conversation.getContact().getCustomFields()).isEmpty();
        verify(contactRepository, never()).save(any());
    }

    @Test
    void doesNothingWhenTheWorkspaceHasNoFieldDefinitions() {
        Conversation conversation = conversationWithDefinedFields();

        writer().apply(
                        conversation,
                        Map.of("orderNumber", "12345"),
                        extractionFields("orderNumber"));

        assertThat(conversation.getContact().getCustomFields()).isEmpty();
        verify(contactRepository, never()).save(any());
    }

    @Test
    void doesNothingWhenExtractedDataIsEmpty() {
        Conversation conversation = conversationWithDefinedFields("orderNumber");

        writer().apply(conversation, Map.of(), extractionFields("orderNumber"));

        assertThat(conversation.getContact().getCustomFields()).isEmpty();
        verify(contactRepository, never()).save(any());
    }

    @Test
    void writesAReservedKeyThatIsNeitherStoredNorDerivable() {
        Conversation conversation = conversationWithDefinedFields();
        Contact contact = conversation.getContact();
        when(externalIdentityRepository.findByContact(contact.getId())).thenReturn(List.of());
        when(reservedContactFieldResolver.resolve(contact, List.of())).thenReturn(Map.of());

        writer().apply(conversation, Map.of("phone", "2348012345678"), extractionFields("phone"));

        assertThat(conversation.getContact().getCustomFields())
                .containsEntry("phone", "2348012345678");
        verify(contactRepository).save(contact);
    }

    @Test
    void doesNotOverwriteAnAlreadyConfiguredValue() {
        Conversation conversation = conversationWithDefinedFields("orderNumber");
        conversation.getContact().getCustomFields().put("orderNumber", "existing-value");

        writer().apply(
                        conversation,
                        Map.of("orderNumber", "new-guess"),
                        extractionFields("orderNumber"));

        assertThat(conversation.getContact().getCustomFields())
                .containsEntry("orderNumber", "existing-value");
        verify(contactRepository, never()).save(any());
    }

    @Test
    void doesNotWriteAReservedKeyThatIsAlreadyDerivableEvenIfNeverExplicitlyStored() {
        Conversation conversation = conversationWithDefinedFields();
        Contact contact = conversation.getContact();
        when(externalIdentityRepository.findByContact(contact.getId())).thenReturn(List.of());
        when(reservedContactFieldResolver.resolve(contact, List.of()))
                .thenReturn(Map.of("displayName", "Ada Lovelace"));

        writer().apply(
                        conversation,
                        Map.of("displayName", "New Name"),
                        extractionFields("displayName"));

        assertThat(conversation.getContact().getCustomFields()).isEmpty();
        verify(contactRepository, never()).save(any());
    }

    @Test
    void ignoresAKeyThatIsWritableButNotAConfiguredExtractionField() {
        Conversation conversation = conversationWithDefinedFields("orderNumber");

        writer().apply(conversation, Map.of("orderNumber", "12345"), extractionFields("sentiment"));

        assertThat(conversation.getContact().getCustomFields()).isEmpty();
        verify(contactRepository, never()).save(any());
    }
}
