package com.relayflow.api.workflow.engine.executor;

import com.relayflow.api.messaging.domain.Contact;
import com.relayflow.api.messaging.domain.ContactFieldDefinition;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.ReservedContactField;
import com.relayflow.api.messaging.repository.ContactRepository;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.workflow.NodeType;
import com.relayflow.api.workflow.engine.ExecutionContext;
import com.relayflow.api.workflow.engine.GraphNode;
import com.relayflow.api.workflow.engine.NodeExecutionException;
import com.relayflow.api.workflow.engine.NodeExecutionResult;
import com.relayflow.api.workflow.engine.NodeExecutor;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sets (or overwrites) a contact's custom field — one of {@link ReservedContactField} or a field
 * the workspace has defined. Unlike {@link com.relayflow.api.agent.ContactCustomFieldWriter}'s
 * passive, gap-filling AI extraction, this node is a deliberate operator-configured action and
 * always overwrites, matching manual edits from the contact detail panel.
 */
@Component
public class SetContactFieldNodeExecutor implements NodeExecutor {

    private final ConversationRepository conversationRepository;

    private final ContactRepository contactRepository;

    public SetContactFieldNodeExecutor(
            ConversationRepository conversationRepository, ContactRepository contactRepository) {
        this.conversationRepository = conversationRepository;
        this.contactRepository = contactRepository;
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
        contactRepository.save(contact);

        return NodeExecutionResult.next(Map.of(key, resolvedValue));
    }
}
