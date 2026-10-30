package io.odunlamizo.relayflow.workflow.engine;

// Docs: docs/codebase-components.md#workflowmessagesender

import io.odunlamizo.relayflow.messaging.OutboundMessageEvent;
import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.messaging.domain.Message;
import io.odunlamizo.relayflow.messaging.domain.MessageDirection;
import io.odunlamizo.relayflow.messaging.domain.MessageSenderType;
import io.odunlamizo.relayflow.messaging.repository.ConversationRepository;
import io.odunlamizo.relayflow.messaging.repository.MessageRepository;
import io.odunlamizo.relayflow.sse.SseBroadcastEvent;
import io.odunlamizo.relayflow.sse.SseEventType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkflowMessageSender {

    private final ConversationRepository conversationRepository;

    private final MessageRepository messageRepository;

    private final ApplicationEventPublisher eventPublisher;

    public WorkflowMessageSender(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ApplicationEventPublisher eventPublisher) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Message send(
            UUID conversationId, UUID workspaceId, String text, List<String> buttonOptions) {
        Conversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(
                                () ->
                                        new NodeExecutionException(
                                                "Conversation not found: " + conversationId));

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
                        workspaceId,
                        SseEventType.MESSAGE_CREATED,
                        Map.of(
                                "workspaceId", workspaceId.toString(),
                                "conversationId", conversationId.toString())));

        eventPublisher.publishEvent(
                new OutboundMessageEvent(message, conversation.getChannelAccount(), buttonOptions));

        return message;
    }
}
