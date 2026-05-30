package com.relayflow.api.workflow.engine.executor;

import com.relayflow.api.messaging.OutboundMessageEvent;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.messaging.repository.MessageRepository;
import com.relayflow.api.sse.SseBroadcastEvent;
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
 * Sends a question to the contact and pauses the workflow run, waiting for their reply.
 *
 * <p>The engine marks the run as {@code WAITING} after this node executes. When the contact
 * replies, {@link com.relayflow.api.workflow.engine.WorkflowResumeListener} resumes the run.
 *
 * <p>Two response modes are supported:
 *
 * <ul>
 *   <li>{@code generic} — the reply text is stored in a named variable and execution continues on
 *       the single default outgoing edge.
 *   <li>{@code defined} — the reply is matched (case-insensitively) against a list of expected
 *       option texts; execution follows the matching option's edge, or the {@code "default"} edge
 *       for unrecognised replies.
 * </ul>
 */
@Component
public class WaitForReplyNodeExecutor implements NodeExecutor {

    private final ConversationRepository conversationRepository;

    private final MessageRepository messageRepository;

    private final ApplicationEventPublisher eventPublisher;

    public WaitForReplyNodeExecutor(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ApplicationEventPublisher eventPublisher) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public NodeType nodeType() {
        return NodeType.WAIT_FOR_REPLY;
    }

    @Override
    @Transactional
    public NodeExecutionResult execute(GraphNode node, ExecutionContext context) {
        String rawQuestion = (String) node.data().get("question");

        if (rawQuestion == null || rawQuestion.isBlank()) {
            throw new NodeExecutionException("Ask Question node has no question configured");
        }

        String text = context.interpolate(rawQuestion);

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
                        "message.created",
                        Map.of(
                                "workspaceId", context.getWorkspaceId().toString(),
                                "conversationId", context.getConversationId().toString())));

        eventPublisher.publishEvent(
                new OutboundMessageEvent(message, conversation.getChannelAccount()));

        return NodeExecutionResult.waiting(
                Map.of("questionMessageId", message.getId().toString(), "question", text));
    }
}
