package com.relayflow.api.workflow.engine;

import com.relayflow.api.messaging.OutboundMessageEvent;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.messaging.repository.MessageRepository;
import com.relayflow.api.sse.SseBroadcastEvent;
import com.relayflow.api.sse.SseEventType;
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
