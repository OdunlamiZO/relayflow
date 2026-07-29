package com.relayflow.api.workflow.engine.executor;

import com.relayflow.api.messaging.OutboundMessageEvent;
import com.relayflow.api.messaging.domain.Conversation;
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
 * Sends a message to the conversation that triggered the workflow.
 *
 * <p>The message is tagged as {@link MessageSenderType#WORKFLOW} so the Telegram adapter delivers
 * it and the trigger listener does not re-fire the workflow.
 */
@Component
public class SendMessageNodeExecutor implements NodeExecutor {

    private final ConversationRepository conversationRepository;

    private final MessageRepository messageRepository;

    private final ApplicationEventPublisher eventPublisher;

    public SendMessageNodeExecutor(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ApplicationEventPublisher eventPublisher) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public NodeType nodeType() {
        return NodeType.SEND_MESSAGE;
    }

    @Override
    @Transactional
    public NodeExecutionResult execute(GraphNode node, ExecutionContext context) {
        String rawMessage = (String) node.data().get("message");

        if (rawMessage == null || rawMessage.isBlank()) {
            throw new NodeExecutionException("Send Message node has no message configured");
        }

        String text = context.interpolate(rawMessage);

        Conversation conversation =
                conversationRepository
                        .findById(context.getConversationId())
                        .orElseThrow(
                                () ->
                                        new NodeExecutionException(
                                                "Conversation not found: "
                                                        + context.getConversationId()));

        Message message = new Message();
        message.setWorkspace(conversation.getWorkspace());
        message.setConversation(conversation);
        message.setDirection(MessageDirection.OUTBOUND);
        message.setSenderType(MessageSenderType.WORKFLOW);
        message.setText(text);
        message.setRawPayload(new LinkedHashMap<>());

        message = messageRepository.save(message);

        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        context.getWorkspaceId(),
                        SseEventType.MESSAGE_CREATED,
                        Map.of(
                                "workspaceId", context.getWorkspaceId().toString(),
                                "conversationId", context.getConversationId().toString())));

        eventPublisher.publishEvent(
                new OutboundMessageEvent(message, conversation.getChannelAccount()));

        return NodeExecutionResult.next(
                Map.of("messageId", message.getId().toString(), "text", text));
    }
}
