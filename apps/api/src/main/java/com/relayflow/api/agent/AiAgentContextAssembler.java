package com.relayflow.api.agent;

import com.relayflow.api.agent.domain.AiAgentConfiguration;
import com.relayflow.api.agent.domain.KnowledgeEntry;
import com.relayflow.api.agent.domain.WorkflowMapping;
import com.relayflow.api.agent.llm.AgentLlmRequest;
import com.relayflow.api.agent.llm.LlmMessage;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.repository.MessageRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AiAgentContextAssembler {

    private static final int HISTORY_LIMIT = 20;

    private final MessageRepository messageRepository;

    public AiAgentContextAssembler(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public AgentLlmRequest assemble(AiAgentConfiguration configuration, Conversation conversation) {
        String systemPrompt = buildSystemPrompt(configuration);
        List<LlmMessage> messages = buildHistory(conversation);

        return new AgentLlmRequest(systemPrompt, messages, null);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private String buildSystemPrompt(AiAgentConfiguration configuration) {
        StringBuilder sb = new StringBuilder();

        if (configuration.getInstructions() != null && !configuration.getInstructions().isBlank()) {
            sb.append(configuration.getInstructions());
        }

        if (!configuration.getWorkflowMappings().isEmpty()) {
            sb.append("\n\n# WORKFLOW ROUTING\n");
            sb.append(
                    "The entries below are automated processes that take priority over your reply."
                            + " When the customer's request clearly matches a description, you MUST"
                            + " trigger that workflow — set \"reply\" to \"\" and add the"
                            + " trigger_workflow action to suggestedActions."
                            + " Never write a reply AND trigger a workflow at the same time."
                            + " If no entry matches, reply directly.\n");
            for (WorkflowMapping mapping : configuration.getWorkflowMappings()) {
                sb.append("\n- trigger_workflow:")
                        .append(mapping.workflowId())
                        .append("  (")
                        .append(mapping.name())
                        .append("): ")
                        .append(mapping.triggerDescription());
            }
        }

        if (!configuration.getKnowledgeBase().isEmpty()) {
            sb.append("\n\n# KNOWLEDGE BASE\n");
            for (KnowledgeEntry entry : configuration.getKnowledgeBase()) {
                sb.append("\nQ: ").append(entry.question());
                sb.append("\nA: ").append(entry.answer());
            }
        }

        return sb.toString().strip();
    }

    private List<LlmMessage> buildHistory(Conversation conversation) {
        List<Message> recent =
                messageRepository.findRecentByConversationSince(
                        conversation.getId(),
                        conversation.getWorkspace().getId(),
                        conversation.getSessionStartedAt(),
                        PageRequest.of(0, HISTORY_LIMIT));

        List<Message> chronological = new ArrayList<>(recent);
        Collections.reverse(chronological);

        List<LlmMessage> messages = new ArrayList<>(chronological.size());

        for (int i = 0; i < chronological.size(); i++) {
            Message msg = chronological.get(i);
            String content = msg.getText() != null ? msg.getText() : "";
            boolean isLast = i == chronological.size() - 1;

            if (isLast && msg.getDirection() == MessageDirection.INBOUND) {
                content = content + buildContactFooter(conversation);
            }

            if (msg.getDirection() == MessageDirection.INBOUND) {
                messages.add(LlmMessage.user(content));
            } else {
                messages.add(LlmMessage.assistant(content));
            }
        }

        return messages;
    }

    private String buildContactFooter(Conversation conversation) {
        String name = conversation.getContact().getDisplayName();
        String contactName = (name != null && !name.isBlank()) ? name : "Unknown";
        String channel = conversation.getChannelAccount().getProvider().name();

        return "\n\n[Contact: " + contactName + " | Channel: " + channel + "]";
    }
}
