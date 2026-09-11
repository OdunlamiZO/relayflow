package com.relayflow.api.workflow.engine.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.contact.repository.ContactRepository;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.webhook.WebhookDispatchService;
import com.relayflow.api.workflow.engine.ExecutionContext;
import com.relayflow.api.workflow.engine.GraphNode;
import com.relayflow.api.workflow.engine.NodeExecutionResult;
import com.relayflow.api.workspace.domain.ContactFieldDefinition;
import com.relayflow.api.workspace.domain.Workspace;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetContactFieldNodeExecutorTest {

    @Mock private ConversationRepository conversationRepository;

    @Mock private ContactRepository contactRepository;

    @Mock private WebhookDispatchService webhookDispatchService;

    private SetContactFieldNodeExecutor executor() {
        return new SetContactFieldNodeExecutor(
                conversationRepository, contactRepository, webhookDispatchService);
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
        conversation.setId(UUID.randomUUID());
        conversation.setWorkspace(workspace);
        conversation.setContact(contact);

        return conversation;
    }

    private ExecutionContext contextFor(Conversation conversation) {
        return new ExecutionContext(
                UUID.randomUUID(), conversation.getId(), UUID.randomUUID(), Map.of());
    }

    @Test
    void writesAReservedFieldEvenWithoutAWorkspaceDefinition() {
        Conversation conversation = conversationWithDefinedFields();
        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));

        NodeExecutionResult result =
                executor()
                        .execute(
                                new GraphNode(
                                        "n1",
                                        "setContactField",
                                        Map.of("fieldKey", "phone", "value", "123")),
                                contextFor(conversation));

        assertThat(conversation.getContact().getCustomFields()).containsEntry("phone", "123");
        assertThat(result.output()).containsEntry("phone", "123");
        verify(contactRepository).save(conversation.getContact());
    }

    @Test
    void writesADefinedCustomField() {
        Conversation conversation = conversationWithDefinedFields("orderNumber");
        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));

        executor()
                .execute(
                        new GraphNode(
                                "n1",
                                "setContactField",
                                Map.of("fieldKey", "orderNumber", "value", "12345")),
                        contextFor(conversation));

        assertThat(conversation.getContact().getCustomFields())
                .containsEntry("orderNumber", "12345");
    }

    @Test
    void overwritesAnAlreadyConfiguredValue() {
        Conversation conversation = conversationWithDefinedFields("orderNumber");
        conversation.getContact().getCustomFields().put("orderNumber", "existing-value");
        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));

        executor()
                .execute(
                        new GraphNode(
                                "n1",
                                "setContactField",
                                Map.of("fieldKey", "orderNumber", "value", "new")),
                        contextFor(conversation));

        assertThat(conversation.getContact().getCustomFields()).containsEntry("orderNumber", "new");
    }

    @Test
    void skipsAKeyThatIsNeitherReservedNorDefined() {
        Conversation conversation = conversationWithDefinedFields();
        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));

        executor()
                .execute(
                        new GraphNode(
                                "n1",
                                "setContactField",
                                Map.of("fieldKey", "randomKey", "value", "x")),
                        contextFor(conversation));

        assertThat(conversation.getContact().getCustomFields()).isEmpty();
        verify(contactRepository, never()).save(any());
    }

    @Test
    void skipsWhenNoFieldKeyConfigured() {
        Conversation conversation = conversationWithDefinedFields();

        NodeExecutionResult result =
                executor()
                        .execute(
                                new GraphNode("n1", "setContactField", Map.of()),
                                contextFor(conversation));

        assertThat(result.output()).containsKey("skipped");
        verify(contactRepository, never()).save(any());
    }

    @Test
    void interpolatesVariablesInTheValue() {
        Conversation conversation = conversationWithDefinedFields();
        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));

        ExecutionContext context =
                new ExecutionContext(
                        UUID.randomUUID(),
                        conversation.getId(),
                        UUID.randomUUID(),
                        Map.of("agent.data.city", "Lagos"));

        executor()
                .execute(
                        new GraphNode(
                                "n1",
                                "setContactField",
                                Map.of("fieldKey", "displayName", "value", "{{agent.data.city}}")),
                        context);

        assertThat(conversation.getContact().getCustomFields())
                .containsEntry("displayName", "Lagos");
    }
}
