package io.odunlamizo.relayflow.agent;

import io.odunlamizo.relayflow.agent.domain.AiAgentConfiguration;
import io.odunlamizo.relayflow.agent.domain.ExtractionField;
import io.odunlamizo.relayflow.agent.domain.KnowledgeEntry;
import io.odunlamizo.relayflow.agent.domain.WorkflowMapping;
import io.odunlamizo.relayflow.agent.llm.AgentLlmRequest;
import io.odunlamizo.relayflow.agent.llm.LlmMessage;
import io.odunlamizo.relayflow.contact.ReservedContactFieldResolver;
import io.odunlamizo.relayflow.contact.domain.Contact;
import io.odunlamizo.relayflow.contact.domain.ExternalIdentity;
import io.odunlamizo.relayflow.contact.repository.ExternalIdentityRepository;
import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.messaging.domain.Message;
import io.odunlamizo.relayflow.messaging.domain.MessageDirection;
import io.odunlamizo.relayflow.messaging.repository.MessageRepository;
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

    private final PublishedWorkflowMappings publishedWorkflowMappings;

    public AiAgentContextAssembler(
            MessageRepository messageRepository,
            ExternalIdentityRepository externalIdentityRepository,
            ReservedContactFieldResolver reservedContactFieldResolver,
            PublishedWorkflowMappings publishedWorkflowMappings) {
        this.messageRepository = messageRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.reservedContactFieldResolver = reservedContactFieldResolver;
        this.publishedWorkflowMappings = publishedWorkflowMappings;
    }

    public AgentLlmRequest assemble(AiAgentConfiguration configuration, Conversation conversation) {
        List<WorkflowMapping> workflowMappings =
                publishedWorkflowMappings.filter(
                        conversation.getWorkspace().getId(), configuration.getWorkflowMappings());
        String systemPrompt = buildSystemPrompt(configuration, workflowMappings);
        List<LlmMessage> messages = buildHistory(configuration, conversation);

        return new AgentLlmRequest(
                systemPrompt, messages, null, configuration.getExtractionFields());
    }

    private String buildSystemPrompt(
            AiAgentConfiguration configuration, List<WorkflowMapping> workflowMappings) {
        StringBuilder sb = new StringBuilder();

        if (configuration.getInstructions() != null && !configuration.getInstructions().isBlank()) {
            sb.append(configuration.getInstructions());
        }

        if (!workflowMappings.isEmpty()) {
            sb.append("\n\n# WORKFLOW ROUTING\n");
            sb.append(
                    "The entries below are automated processes that take priority over your reply."
                            + " When the customer's request clearly matches a description, you MUST"
                            + " trigger that workflow — set \"reply\" to \"\" and add the"
                            + " trigger_workflow action to suggestedActions."
                            + " Never write a reply AND trigger a workflow at the same time."
                            + " If no entry matches, reply directly.\n");
            for (WorkflowMapping mapping : workflowMappings) {
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

        for (int index = 0; index < chronological.size(); index++) {
            Message message = chronological.get(index);
            String content = message.getText() != null ? message.getText() : "";
            boolean isLast = index == chronological.size() - 1;

            if (isLast && message.getDirection() == MessageDirection.INBOUND) {
                content = content + buildContactFooter(configuration, conversation);
            }

            if (message.getDirection() == MessageDirection.INBOUND) {
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
