package io.odunlamizo.relayflow.workflow.engine.executor;

import io.odunlamizo.relayflow.messaging.domain.Message;
import io.odunlamizo.relayflow.workflow.NodeType;
import io.odunlamizo.relayflow.workflow.engine.ExecutionContext;
import io.odunlamizo.relayflow.workflow.engine.GraphNode;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutionException;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutionResult;
import io.odunlamizo.relayflow.workflow.engine.NodeExecutor;
import io.odunlamizo.relayflow.workflow.engine.WorkflowMessageSender;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

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
