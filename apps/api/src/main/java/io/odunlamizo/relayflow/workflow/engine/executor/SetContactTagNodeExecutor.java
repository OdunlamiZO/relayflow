package io.odunlamizo.relayflow.workflow.engine.executor;

// Docs: docs/codebase-components.md#setcontacttagnodeexecutor

import io.odunlamizo.relayflow.contact.ContactTagService;
import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.messaging.repository.ConversationRepository;
import io.odunlamizo.relayflow.workflow.NodeType;
import io.odunlamizo.relayflow.workflow.engine.ExecutionContext;
import io.odunlamizo.relayflow.workflow.engine.GraphNode;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutionException;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutionResult;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutor;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SetContactTagNodeExecutor implements NodeExecutor {

    private final ConversationRepository conversationRepository;

    private final ContactTagService contactTagService;

    public SetContactTagNodeExecutor(
            ConversationRepository conversationRepository, ContactTagService contactTagService) {
        this.conversationRepository = conversationRepository;
        this.contactTagService = contactTagService;
    }

    @Override
    public NodeType nodeType() {
        return NodeType.SET_CONTACT_TAG;
    }

    @Override
    @Transactional
    public NodeExecutionResult execute(GraphNode node, ExecutionContext context) {
        String tagKey = (String) node.data().get("tagKey");

        if (tagKey == null || tagKey.isBlank()) {
            return NodeExecutionResult.next(Map.of("skipped", "no contact tag configured"));
        }

        String rawValue = (String) node.data().get("value");
        String value = context.interpolate(rawValue != null ? rawValue : "");

        Conversation conversation =
                conversationRepository
                        .findById(context.getConversationId())
                        .orElseThrow(
                                () ->
                                        new NodeExecutionException(
                                                "Conversation not found: "
                                                        + context.getConversationId()));

        try {
            contactTagService.applyTags(conversation.getContact(), Map.of(tagKey, value));
        } catch (ResponseStatusException exception) {
            throw new NodeExecutionException(exception.getReason());
        }

        context.setVariable("contact.tags." + tagKey, value);

        return NodeExecutionResult.next(Map.of(tagKey, value));
    }
}
