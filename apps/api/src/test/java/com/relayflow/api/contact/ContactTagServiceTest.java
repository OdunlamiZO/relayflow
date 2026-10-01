package com.relayflow.api.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.contact.repository.ContactRepository;
import com.relayflow.api.webhook.WebhookDispatchService;
import com.relayflow.api.webhook.domain.WebhookEventType;
import com.relayflow.api.workspace.domain.ContactTagDefinition;
import com.relayflow.api.workspace.domain.Workspace;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ContactTagServiceTest {

    @Mock private ContactRepository contactRepository;

    @Mock private WebhookDispatchService webhookDispatchService;

    private Contact contact;

    @BeforeEach
    void setUp() {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setContactTagDefinitions(
                List.of(
                        new ContactTagDefinition(
                                "kyc_status", "KYC status", List.of("pending", "verified"))));

        contact = new Contact();
        contact.setId(UUID.randomUUID());
        contact.setWorkspace(workspace);
    }

    private ContactTagService service() {
        return new ContactTagService(contactRepository, webhookDispatchService);
    }

    @Test
    void setsAnAllowedValueAndSendsTheContactUpdatedWebhook() {
        service().applyTags(contact, Map.of("kyc_status", "verified"));

        assertThat(contact.getTags()).containsEntry("kyc_status", "verified");
        verify(contactRepository).save(contact);
        verify(webhookDispatchService)
                .dispatch(
                        eq(contact.getWorkspace().getId()),
                        eq(WebhookEventType.CONTACT_UPDATED),
                        any());
    }

    @Test
    void blankOrNullValueClearsTheTag() {
        contact.setTags(new HashMap<>(Map.of("kyc_status", "pending")));
        Map<String, String> changes = new HashMap<>();
        changes.put("kyc_status", null);

        service().applyTags(contact, changes);

        assertThat(contact.getTags()).doesNotContainKey("kyc_status");
    }

    @Test
    void rejectsATagThatIsNotDefined() {
        assertThatThrownBy(() -> service().applyTags(contact, Map.of("tier", "gold")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("\"tier\" is not a contact tag");
    }

    @Test
    void rejectsAValueThatIsNotAllowed() {
        assertThatThrownBy(() -> service().applyTags(contact, Map.of("kyc_status", "maybe")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Allowed values: pending, verified");
    }

    @Test
    void unchangedTagsAreNotSavedAgain() {
        contact.setTags(new HashMap<>(Map.of("kyc_status", "verified")));

        service().applyTags(contact, Map.of("kyc_status", "verified"));

        verify(contactRepository, never()).save(any());
        verify(webhookDispatchService, never()).dispatch(any(), any(), any());
    }
}
