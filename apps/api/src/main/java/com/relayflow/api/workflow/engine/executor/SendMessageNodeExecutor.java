package com.relayflow.api.workflow.engine.executor;

import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.MessageSenderType;
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
 * Sends a message to the conversation that triggered the workflow.
 *
 * <p>The message is tagged as {@link MessageSenderType#WORKFLOW} so the Telegram adapter delivers
 * it and the trigger listener does not re-fire the workflow.
 */
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
