package com.relayflow.api.workflow.engine.executor;

import com.relayflow.api.messaging.OutboundMessageEvent;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.ConversationStatus;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.messaging.repository.MessageRepository;
import com.relayflow.api.sse.SseBroadcastEvent;
import com.relayflow.api.sse.SseEventType;
import com.relayflow.api.workflow.NodeType;
import com.relayflow.api.workflow.engine.ExecutionContext;
import com.relayflow.api.workflow.engine.GraphNode;
import com.relayflow.api.workflow.engine.NodeExecutionException;
import com.relayflow.api.workflow.engine.NodeExecutionResult;
import com.relayflow.api.workflow.engine.NodeExecutor;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Closes the conversation that triggered the workflow and optionally sends a final message.
 *
 * <p>This node is terminal — it has no outgoing handle, so the workflow run ends here. The
 * conversation status is set to {@link ConversationStatus#CLOSED} and the inbox is notified via SSE
 * so the UI updates immediately.
 */
@Component
public class EndConversationNodeExecutor implements NodeExecutor {

    private final ConversationRepository conversationRepository;

    private final MessageRepository messageRepository;

    private final ApplicationEventPublisher eventPublisher;

    public EndConversationNodeExecutor(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ApplicationEventPublisher eventPublisher) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public NodeType nodeType() {
        return NodeType.END_CONVERSATION;
    }

    @Override
    @Transactional
    public NodeExecutionResult execute(GraphNode node, ExecutionContext context) {
        Conversation conversation =
                conversationRepository
                        .findById(context.getConversationId())
                        .orElseThrow(
                                () ->
                                        new NodeExecutionException(
                                                "Conversation not found: "
                                                        + context.getConversationId()));

        Map<String, Object> output = new LinkedHashMap<>();

        // Optional closing message — send it before closing the conversation.
        String rawMessage = (String) node.data().get("message");

        if (rawMessage != null && !rawMessage.isBlank()) {
            String text = context.interpolate(rawMessage);

            Message message = new Message();
            message.setWorkspace(conversation.getWorkspace());
            message.setConversation(conversation);
            message.setDirection(MessageDirection.OUTBOUND);
            message.setSenderType(MessageSenderType.WORKFLOW);
            message.setText(text);
            message.setRawPayload(new LinkedHashMap<>());

            message = messageRepository.save(message);

            conversation.setLastMessageAt(Instant.now());

            eventPublisher.publishEvent(
                    new OutboundMessageEvent(message, conversation.getChannelAccount()));

            eventPublisher.publishEvent(
                    new SseBroadcastEvent(
                            context.getWorkspaceId(),
                            SseEventType.MESSAGE_CREATED,
                            Map.of(
                                    "workspaceId", context.getWorkspaceId().toString(),
                                    "conversationId", context.getConversationId().toString())));

            output.put("messageId", message.getId().toString());
            output.put("text", text);
        }

        // Close the conversation, release workflow ownership, and unassign it.
        conversation.setStatus(ConversationStatus.CLOSED);
        conversation.setLockedByWorkflow(false);
        conversation.setAssigneeId(null);
        conversationRepository.save(conversation);

        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        context.getWorkspaceId(),
                        SseEventType.CONVERSATION_UPDATED,
                        Map.of(
                                "workspaceId", context.getWorkspaceId().toString(),
                                "conversationId", context.getConversationId().toString())));

        output.put("conversationStatus", "CLOSED");

        return NodeExecutionResult.next(output);
    }
}
