package com.relayflow.api.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.relayflow.api.channel.ChannelAccountService;
import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.contact.repository.ContactRepository;
import com.relayflow.api.contact.repository.ExternalIdentityRepository;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.webhook.WebhookDispatchService;
import com.relayflow.api.webhook.domain.WebhookEventType;
import com.relayflow.api.webhook.dto.ContactMergedPayload;
import com.relayflow.api.webhook.dto.ContactSnapshot;
import com.relayflow.api.workspace.WorkspaceService;
import com.relayflow.api.workspace.domain.Workspace;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    @Mock private ContactRepository contactRepository;

    @Mock private ExternalIdentityRepository externalIdentityRepository;

    @Mock private ConversationRepository conversationRepository;

    @Mock private WorkspaceService workspaceService;

    @Mock private ChannelAccountService channelAccountService;

    @Mock private ReservedContactFieldResolver reservedContactFieldResolver;

    @Mock private WebhookDispatchService webhookDispatchService;

    @Mock private ContactTagService contactTagService;

    private ContactService service;

    private Workspace workspace;

    @BeforeEach
    void setUp() {
        service =
                new ContactService(
                        contactRepository,
                        externalIdentityRepository,
                        conversationRepository,
                        new ContactMapperImpl(),
                        workspaceService,
                        channelAccountService,
                        reservedContactFieldResolver,
                        webhookDispatchService,
                        contactTagService);
        workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
    }

    @Test
    void sendsContactDeletedWithTheDeletedContact() {
        Contact contact = contact("Ada", Map.of("phone", "+14155550123"));
        when(contactRepository.findById(contact.getId())).thenReturn(Optional.of(contact));

        service.deleteContact(contact.getId(), WORKSPACE_ID);

        verify(contactRepository).delete(contact);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(webhookDispatchService)
                .dispatch(
                        eq(WORKSPACE_ID), eq(WebhookEventType.CONTACT_DELETED), payload.capture());
        assertThat(((ContactSnapshot) payload.getValue()).contact())
                .containsEntry("id", contact.getId().toString())
                .containsEntry("displayName", "Ada")
                .containsEntry("phone", "+14155550123");
    }

    @Test
    void sendsContactMergedWithBothContacts() {
        Contact target = contact("Ada", Map.of());
        Contact source = contact("Ada L.", Map.of("email", "ada@example.com"));
        when(contactRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(contactRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(externalIdentityRepository.findByContact(source.getId())).thenReturn(List.of());
        when(externalIdentityRepository.findByContact(target.getId())).thenReturn(List.of());
        when(externalIdentityRepository.findByContacts(List.of(target.getId())))
                .thenReturn(List.of());

        service.mergeContacts(target.getId(), source.getId(), WORKSPACE_ID);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(webhookDispatchService)
                .dispatch(eq(WORKSPACE_ID), eq(WebhookEventType.CONTACT_MERGED), payload.capture());
        ContactMergedPayload merged = (ContactMergedPayload) payload.getValue();
        assertThat(merged.contact()).containsEntry("id", target.getId().toString());
        assertThat(merged.mergedContact())
                .containsEntry("id", source.getId().toString())
                .containsEntry("email", "ada@example.com");
    }

    private Contact contact(String displayName, Map<String, String> customFields) {
        Contact contact = new Contact();
        contact.setId(UUID.randomUUID());
        contact.setWorkspace(workspace);
        contact.setDisplayName(displayName);
        contact.setCustomFields(customFields);

        return contact;
    }
}
