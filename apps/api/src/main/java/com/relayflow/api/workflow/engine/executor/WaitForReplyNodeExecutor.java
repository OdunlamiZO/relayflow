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
import java.util.List;
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

    private static final long DEFAULT_TIMEOUT_MINUTES = 60 * 24;

    private static final long MAX_TIMEOUT_MINUTES = 7 * 24 * 60;

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
                        SseEventType.MESSAGE_CREATED,
                        Map.of(
                                "workspaceId", context.getWorkspaceId().toString(),
                                "conversationId", context.getConversationId().toString())));

        List<String> buttonOptions = extractButtonOptions(node);

        eventPublisher.publishEvent(
                new OutboundMessageEvent(message, conversation.getChannelAccount(), buttonOptions));

        long timeoutMinutes =
                node.data().get("timeoutMinutes") instanceof Number n
                        ? n.longValue()
                        : DEFAULT_TIMEOUT_MINUTES;
        timeoutMinutes = Math.min(timeoutMinutes, MAX_TIMEOUT_MINUTES);

        return NodeExecutionResult.waiting(
                Map.of("questionMessageId", message.getId().toString(), "question", text),
                timeoutMinutes * 60);
    }

    /**
     * Returns the list of option texts for a {@code defined} response node so channel adapters can
     * render them as interactive buttons / keyboard shortcuts. Returns an empty list for {@code
     * generic} nodes.
     */
    @SuppressWarnings("unchecked")
    private List<String> extractButtonOptions(GraphNode node) {
        String responseType = (String) node.data().getOrDefault("responseType", "generic");

        if (!"defined".equals(responseType)) {
            return List.of();
        }

        List<Map<String, Object>> options =
                (List<Map<String, Object>>) node.data().getOrDefault("options", List.of());

        return options.stream()
                .map(option -> (String) option.get("text"))
                .filter(text -> text != null && !text.isBlank())
                .toList();
    }
}
