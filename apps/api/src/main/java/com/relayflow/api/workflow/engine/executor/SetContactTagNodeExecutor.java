package com.relayflow.api.workflow.engine.executor;

import com.relayflow.api.contact.ContactTagService;
import com.relayflow.api.messaging.domain.Conversation;
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
        } catch (ResponseStatusException e) {
            throw new NodeExecutionException(e.getReason());
        }

        context.setVariable("contact.tags." + tagKey, value);

        return NodeExecutionResult.next(Map.of(tagKey, value));
    }
}
