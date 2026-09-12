package com.relayflow.api.agent;

import com.relayflow.api.agent.domain.AiAgentConfiguration;
import com.relayflow.api.agent.domain.ExtractionField;
import com.relayflow.api.agent.domain.KnowledgeEntry;
import com.relayflow.api.agent.domain.WorkflowMapping;
import com.relayflow.api.agent.llm.AgentLlmRequest;
import com.relayflow.api.agent.llm.LlmMessage;
import com.relayflow.api.contact.ReservedContactFieldResolver;
import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.contact.domain.ExternalIdentity;
import com.relayflow.api.contact.repository.ExternalIdentityRepository;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.repository.MessageRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AiAgentContextAssembler {

    private static final int HISTORY_LIMIT = 20;

    private final MessageRepository messageRepository;

    private final ExternalIdentityRepository externalIdentityRepository;

    private final ReservedContactFieldResolver reservedContactFieldResolver;

    public AiAgentContextAssembler(
            MessageRepository messageRepository,
            ExternalIdentityRepository externalIdentityRepository,
            ReservedContactFieldResolver reservedContactFieldResolver) {
        this.messageRepository = messageRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.reservedContactFieldResolver = reservedContactFieldResolver;
    }

    public AgentLlmRequest assemble(AiAgentConfiguration configuration, Conversation conversation) {
        String systemPrompt = buildSystemPrompt(configuration);
        List<LlmMessage> messages = buildHistory(configuration, conversation);

        return new AgentLlmRequest(
                systemPrompt, messages, null, configuration.getExtractionFields());
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

    private List<LlmMessage> buildHistory(
            AiAgentConfiguration configuration, Conversation conversation) {
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
                content = content + buildContactFooter(configuration, conversation);
            }

            if (msg.getDirection() == MessageDirection.INBOUND) {
                messages.add(LlmMessage.user(content));
            } else {
                messages.add(LlmMessage.assistant(content));
            }
        }

        return messages;
    }

    private String buildContactFooter(
            AiAgentConfiguration configuration, Conversation conversation) {
        String name = conversation.getContact().getDisplayName();
        String contactName = (name != null && !name.isBlank()) ? name : "Unknown";
        String channel = conversation.getChannelAccount().getProvider().name();

        StringBuilder footer =
                new StringBuilder("\n\n<context>Contact: ")
                        .append(contactName)
                        .append(" | Channel: ")
                        .append(channel);

        List<ExtractionField> extractionFields = configuration.getExtractionFields();

        if (!extractionFields.isEmpty()) {
            Map<String, String> effectiveValues = effectiveContactFields(conversation.getContact());

            String known =
                    extractionFields.stream()
                            .filter(field -> hasEffectiveValue(effectiveValues, field.key()))
                            .map(field -> field.key() + "=" + effectiveValues.get(field.key()))
                            .collect(Collectors.joining(", "));

            String missing =
                    extractionFields.stream()
                            .filter(field -> !hasEffectiveValue(effectiveValues, field.key()))
                            .map(ExtractionField::key)
                            .collect(Collectors.joining(", "));

            if (!known.isEmpty()) {
                footer.append(" | Already known: ").append(known);
            }

            if (!missing.isEmpty()) {
                footer.append(" | Still missing: ").append(missing);
            }
        }

        return footer.append("</context>").toString();
    }

    /**
     * Merges {@link ReservedContactFieldResolver}'s auto-derived values with the contact's
     * explicitly-stored custom fields — an explicitly-stored value always wins. Same precedence as
     * {@code ContactService.getContactDetail}.
     */
    private Map<String, String> effectiveContactFields(Contact contact) {
        List<ExternalIdentity> identities =
                externalIdentityRepository.findByContact(contact.getId());

        Map<String, String> effectiveValues =
                new LinkedHashMap<>(reservedContactFieldResolver.resolve(contact, identities));
        effectiveValues.putAll(contact.getCustomFields());

        return effectiveValues;
    }

    private boolean hasEffectiveValue(Map<String, String> effectiveValues, String key) {
        String value = effectiveValues.get(key);

        return value != null && !value.isBlank();
    }
}
