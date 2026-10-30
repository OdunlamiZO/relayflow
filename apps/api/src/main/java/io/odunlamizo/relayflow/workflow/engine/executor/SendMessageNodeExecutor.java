package io.odunlamizo.relayflow.workflow.engine.executor;

// Docs: docs/codebase-components.md#sendmessagenodeexecutor

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

/** Tagged as WORKFLOW so the trigger listener does not re-fire the workflow. */
@Component
public class SendMessageNodeExecutor implements NodeExecutor {

    private final WorkflowMessageSender messageSender;

    public SendMessageNodeExecutor(WorkflowMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public NodeType nodeType() {
        return NodeType.SEND_MESSAGE;
    }

    @Override
    public NodeExecutionResult execute(GraphNode node, ExecutionContext context) {
        String rawMessage = (String) node.data().get("message");

        if (rawMessage == null || rawMessage.isBlank()) {
            throw new NodeExecutionException("Send Message node has no message configured");
        }

        String text = context.interpolate(rawMessage);

        Message message =
                messageSender.send(
                        context.getConversationId(), context.getWorkspaceId(), text, List.of());

        return NodeExecutionResult.next(
                Map.of("messageId", message.getId().toString(), "text", text));
    }
}
