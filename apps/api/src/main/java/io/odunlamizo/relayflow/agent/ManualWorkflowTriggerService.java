package io.odunlamizo.relayflow.agent;

// Docs: docs/codebase-components.md#manualworkflowtriggerservice

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.agent.domain.AiAgentConfiguration;
import io.odunlamizo.relayflow.agent.domain.AiAgentInvocationStatus;
import io.odunlamizo.relayflow.agent.domain.ConversationAiDraft;
import io.odunlamizo.relayflow.agent.domain.WorkflowMapping;
import io.odunlamizo.relayflow.agent.repository.AiAgentConfigurationRepository;
import io.odunlamizo.relayflow.agent.repository.AiAgentInvocationLogRepository;
import io.odunlamizo.relayflow.agent.repository.ConversationAiDraftRepository;
import io.odunlamizo.relayflow.channel.repository.ChannelAccountRepository;
import io.odunlamizo.relayflow.common.ResourceNotFoundException;
import io.odunlamizo.relayflow.messaging.ConversationNotFoundException;
import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.messaging.domain.ConversationStatus;
import io.odunlamizo.relayflow.messaging.repository.ConversationRepository;
import io.odunlamizo.relayflow.sse.SseBroadcastEvent;
import io.odunlamizo.relayflow.sse.SseEventType;
import io.odunlamizo.relayflow.workflow.domain.WorkflowDefinition;
import io.odunlamizo.relayflow.workflow.domain.WorkflowRunStatus;
import io.odunlamizo.relayflow.workflow.engine.WorkflowEngineService;
import io.odunlamizo.relayflow.workflow.repository.WorkflowDefinitionRepository;
import io.odunlamizo.relayflow.workflow.repository.WorkflowRunRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Lets a person start one of the AI agent's workflows when the agent couldn't pick one. */
@Service
public class ManualWorkflowTriggerService {

    private static final Logger log = LoggerFactory.getLogger(ManualWorkflowTriggerService.class);

    private final ConversationRepository conversationRepository;

    private final ChannelAccountRepository channelAccountRepository;

    private final AiAgentConfigurationRepository configurationRepository;

    private final PublishedWorkflowMappings publishedWorkflowMappings;

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    private final WorkflowRunRepository workflowRunRepository;

    private final AiAgentInvocationLogRepository invocationLogRepository;

    private final ConversationAiDraftRepository draftRepository;

    private final WorkflowEngineService workflowEngineService;

    private final ApplicationEventPublisher eventPublisher;

    public ManualWorkflowTriggerService(
            ConversationRepository conversationRepository,
            ChannelAccountRepository channelAccountRepository,
            AiAgentConfigurationRepository configurationRepository,
            PublishedWorkflowMappings publishedWorkflowMappings,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowRunRepository workflowRunRepository,
            AiAgentInvocationLogRepository invocationLogRepository,
            ConversationAiDraftRepository draftRepository,
            WorkflowEngineService workflowEngineService,
            ApplicationEventPublisher eventPublisher) {
        this.conversationRepository = conversationRepository;
        this.channelAccountRepository = channelAccountRepository;
        this.configurationRepository = configurationRepository;
        this.publishedWorkflowMappings = publishedWorkflowMappings;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.invocationLogRepository = invocationLogRepository;
        this.draftRepository = draftRepository;
        this.workflowEngineService = workflowEngineService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<WorkflowMapping> listWorkflows(UUID workspaceId, UUID conversationId) {
        Conversation conversation = conversation(workspaceId, conversationId);

        return workflows(workspaceId, agentConfiguration(workspaceId, conversation));
    }

    @Transactional
    public void runWorkflow(UUID workspaceId, UUID conversationId, UUID workflowId) {
        Conversation conversation = conversation(workspaceId, conversationId);

        if (conversation.getStatus() == ConversationStatus.CLOSED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This conversation is closed. Reopen it to run a workflow.");
        }

        Optional<AiAgentConfiguration> configuration =
                agentConfiguration(workspaceId, conversation);
        boolean mapped =
                workflows(workspaceId, configuration).stream()
                        .anyMatch(mapping -> mapping.workflowId().equals(workflowId));

        if (!mapped) {
            throw new ResourceNotFoundException(
                    "This workflow isn't one of the AI agent's published workflows");
        }

        if (hasActiveRun(conversationId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A workflow is already running in this conversation. Close the conversation"
                            + " to stop it.");
        }

        if (invocationLogRepository
                .findForConversationWithStatus(conversationId, AiAgentInvocationStatus.RUNNING)
                .isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "The AI agent is replying. Try again in a moment.");
        }

        invocationLogRepository
                .findForConversationWithStatus(conversationId, AiAgentInvocationStatus.CLARIFYING)
                .ifPresent(
                        clarifying -> {
                            clarifying.setStatus(AiAgentInvocationStatus.SENT);
                            clarifying.setFinishedAt(Instant.now());
                            invocationLogRepository.save(clarifying);
                        });

        Optional<ConversationAiDraft> draft = draftRepository.findByConversation(conversationId);
        Map<String, String> extractedData =
                draft.map(ConversationAiDraft::getExtractedData).orElse(Map.of());
        draft.ifPresent(draftRepository::delete);

        conversation.setEscalatedAt(null);
        conversation.setEscalationReason(null);
        conversation.setEscalationType(null);
        conversation.setLockedByAiAgent(false);
        conversationRepository.save(conversation);

        WorkflowDefinition workflowDefinition =
                workflowDefinitionRepository
                        .findInWorkspace(workflowId, workspaceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
        Map<String, String> agentContext =
                AgentWorkflowContext.build(
                        null,
                        null,
                        extractedData,
                        configuration
                                .map(AiAgentConfiguration::getExtractionFields)
                                .orElse(List.of()));

        log.info(
                "Workflow started by a person {} {} {}",
                kv("workspaceId", workspaceId),
                kv("conversationId", conversationId),
                kv("workflowId", workflowId));

        workflowEngineService.executeWorkflow(workflowDefinition, conversation, null, agentContext);

        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        workspaceId,
                        SseEventType.CONVERSATION_UPDATED,
                        Map.of(
                                "workspaceId", workspaceId.toString(),
                                "conversationId", conversationId.toString())));
    }

    private Conversation conversation(UUID workspaceId, UUID conversationId) {
        return conversationRepository
                .findInWorkspace(conversationId, workspaceId)
                .orElseThrow(ConversationNotFoundException::new);
    }

    private Optional<AiAgentConfiguration> agentConfiguration(
            UUID workspaceId, Conversation conversation) {
        return channelAccountRepository
                .findAiAgentConfiguration(conversation.getChannelAccount().getId())
                .or(() -> configurationRepository.findDefault(workspaceId));
    }

    private List<WorkflowMapping> workflows(
            UUID workspaceId, Optional<AiAgentConfiguration> configuration) {
        return configuration
                .map(
                        found ->
                                publishedWorkflowMappings.filter(
                                        workspaceId, found.getWorkflowMappings()))
                .orElse(List.of());
    }

    private boolean hasActiveRun(UUID conversationId) {
        return !workflowRunRepository
                        .findForConversationWithStatus(conversationId, WorkflowRunStatus.RUNNING)
                        .isEmpty()
                || !workflowRunRepository
                        .findForConversationWithStatus(conversationId, WorkflowRunStatus.WAITING)
                        .isEmpty();
    }
}
