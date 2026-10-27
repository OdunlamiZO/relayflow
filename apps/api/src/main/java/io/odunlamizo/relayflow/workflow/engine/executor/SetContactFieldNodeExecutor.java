package io.odunlamizo.relayflow.workflow.engine.executor;

import io.odunlamizo.relayflow.contact.ContactDisplayNameSync;
import io.odunlamizo.relayflow.contact.domain.Contact;
import io.odunlamizo.relayflow.contact.repository.ContactRepository;
import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.messaging.repository.ConversationRepository;
import io.odunlamizo.relayflow.webhook.ContactSnapshotBuilder;
import io.odunlamizo.relayflow.webhook.WebhookDispatchService;
import io.odunlamizo.relayflow.webhook.domain.WebhookEventType;
import io.odunlamizo.relayflow.workflow.NodeType;
import io.odunlamizo.relayflow.workflow.engine.ExecutionContext;
import io.odunlamizo.relayflow.workflow.engine.GraphNode;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutionException;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutionResult;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutor;
import io.odunlamizo.relayflow.workspace.domain.ContactFieldDefinition;
import io.odunlamizo.relayflow.workspace.domain.ReservedContactField;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SetContactFieldNodeExecutor implements NodeExecutor {

    private final ConversationRepository conversationRepository;

    private final ContactRepository contactRepository;

    private final WebhookDispatchService webhookDispatchService;

    public SetContactFieldNodeExecutor(
            ConversationRepository conversationRepository,
            ContactRepository contactRepository,
            WebhookDispatchService webhookDispatchService) {
        this.conversationRepository = conversationRepository;
        this.contactRepository = contactRepository;
        this.webhookDispatchService = webhookDispatchService;
    }

    @Override
    public NodeType nodeType() {
        return NodeType.SET_CONTACT_FIELD;
    }

    @Override
    @Transactional
    public NodeExecutionResult execute(GraphNode node, ExecutionContext context) {
        String fieldKey = (String) node.data().get("fieldKey");

        if (fieldKey == null || fieldKey.isBlank()) {
            return NodeExecutionResult.next(Map.of("skipped", "no contact field configured"));
        }

        Conversation conversation =
                conversationRepository
                        .findById(context.getConversationId())
                        .orElseThrow(
                                () ->
                                        new NodeExecutionException(
                                                "Conversation not found: "
                                                        + context.getConversationId()));

        String key = fieldKey.trim();

        boolean writable =
                ReservedContactField.isReserved(key)
                        || conversation.getWorkspace().getContactFieldDefinitions().stream()
                                .map(ContactFieldDefinition::key)
                                .anyMatch(key::equals);

        if (!writable) {
            return NodeExecutionResult.next(
                    Map.of("skipped", "\"" + key + "\" is not a defined contact field"));
        }

        String rawValue = (String) node.data().get("value");
        String resolvedValue = context.interpolate(rawValue != null ? rawValue : "");

        Contact contact = conversation.getContact();
        contact.getCustomFields().put(key, resolvedValue);
        ContactDisplayNameSync.apply(contact);
        contactRepository.save(contact);

        webhookDispatchService.dispatch(
                conversation.getWorkspace().getId(),
                WebhookEventType.CONTACT_UPDATED,
                ContactSnapshotBuilder.build(contact));

        return NodeExecutionResult.next(Map.of(key, resolvedValue));
    }
}
