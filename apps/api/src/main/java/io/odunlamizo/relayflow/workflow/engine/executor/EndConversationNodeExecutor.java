package io.odunlamizo.relayflow.workflow.engine.executor;

// Docs: docs/codebase-components.md#endconversationnodeexecutor

import io.odunlamizo.relayflow.messaging.OutboundMessageEvent;
import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.messaging.domain.ConversationStatus;
import io.odunlamizo.relayflow.messaging.domain.Message;
import io.odunlamizo.relayflow.messaging.domain.MessageDirection;
import io.odunlamizo.relayflow.messaging.domain.MessageSenderType;
import io.odunlamizo.relayflow.messaging.repository.ConversationRepository;
import io.odunlamizo.relayflow.messaging.repository.MessageRepository;
import io.odunlamizo.relayflow.sse.SseBroadcastEvent;
import io.odunlamizo.relayflow.sse.SseEventType;
import io.odunlamizo.relayflow.workflow.NodeType;
import io.odunlamizo.relayflow.workflow.engine.ExecutionContext;
import io.odunlamizo.relayflow.workflow.engine.GraphNode;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutionException;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutionResult;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutor;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
