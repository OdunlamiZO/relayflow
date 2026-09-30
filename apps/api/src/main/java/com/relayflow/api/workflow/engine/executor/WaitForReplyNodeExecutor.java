package com.relayflow.api.workflow.engine.executor;

import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.workflow.NodeType;
import com.relayflow.api.workflow.engine.ExecutionContext;
import com.relayflow.api.workflow.engine.GraphNode;
import com.relayflow.api.workflow.engine.NodeExecutionException;
import com.relayflow.api.workflow.engine.NodeExecutionResult;
import com.relayflow.api.workflow.engine.NodeExecutor;
import com.relayflow.api.workflow.engine.WorkflowMessageSender;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

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
 *       the single default outgoing edge. With a {@code validationHook}, it branches into {@code
 *       valid} / {@code invalid} instead.
 *   <li>{@code defined} — the reply is matched (case-insensitively) against a list of expected
 *       option texts; execution follows the matching option's edge, or the {@code "default"} edge
 *       for unrecognised replies.
 * </ul>
 */
@Component
public class WaitForReplyNodeExecutor implements NodeExecutor {

    private static final long DEFAULT_TIMEOUT_MINUTES = 60 * 24;

    private static final long MAX_TIMEOUT_MINUTES = 7 * 24 * 60;

    private final WorkflowMessageSender messageSender;

    public WaitForReplyNodeExecutor(WorkflowMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public NodeType nodeType() {
        return NodeType.WAIT_FOR_REPLY;
    }

    @Override
    public NodeExecutionResult execute(GraphNode node, ExecutionContext context) {
        String rawQuestion = (String) node.data().get("question");

        if (rawQuestion == null || rawQuestion.isBlank()) {
            throw new NodeExecutionException("Ask Question node has no question configured");
        }

        String text = context.interpolate(rawQuestion);

        Message message =
                messageSender.send(
                        context.getConversationId(),
                        context.getWorkspaceId(),
                        text,
                        extractButtonOptions(node));

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
