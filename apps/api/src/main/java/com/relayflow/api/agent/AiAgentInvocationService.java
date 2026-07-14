package com.relayflow.api.agent;

import com.relayflow.api.agent.domain.AiAgentConfiguration;
import com.relayflow.api.agent.domain.AiAgentInvocationLog;
import com.relayflow.api.agent.domain.AiAgentInvocationStatus;
import com.relayflow.api.agent.domain.AutonomyCeiling;
import com.relayflow.api.agent.domain.ConversationAiDraft;
import com.relayflow.api.agent.domain.ExtractionField;
import com.relayflow.api.agent.llm.AgentLlmRequest;
import com.relayflow.api.agent.llm.AgentLlmResponse;
import com.relayflow.api.agent.llm.LlmClientFactory;
import com.relayflow.api.agent.repository.AiAgentConfigurationRepository;
import com.relayflow.api.agent.repository.AiAgentInvocationLogRepository;
import com.relayflow.api.agent.repository.ConversationAiDraftRepository;
import com.relayflow.api.messaging.MessagingService;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.dto.CreateMessageRequest;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.sse.SseBroadcastEvent;
import com.relayflow.api.workflow.domain.WorkflowDefinition;
import com.relayflow.api.workflow.domain.WorkflowRunStatus;
import com.relayflow.api.workflow.engine.WorkflowEngineService;
import com.relayflow.api.workflow.repository.WorkflowDefinitionRepository;
import com.relayflow.api.workflow.repository.WorkflowRunRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAgentInvocationService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentInvocationService.class);

    private static final String WORKFLOW_ACTION_PREFIX = "trigger_workflow:";

    private final AiAgentConfigurationRepository configurationRepository;

    private final AiAgentInvocationLogRepository invocationLogRepository;

    private final ConversationAiDraftRepository draftRepository;

    private final WorkflowRunRepository workflowRunRepository;

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    private final WorkflowEngineService workflowEngineService;

    private final AiAgentContextAssembler contextAssembler;

    private final LlmClientFactory llmClientFactory;

    private final MessagingService messagingService;

    private final ConversationRepository conversationRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final AiAgentInvocationSlotClaimer slotClaimer;

    private final ContactCustomFieldWriter contactCustomFieldWriter;

    public AiAgentInvocationService(
            AiAgentConfigurationRepository configurationRepository,
            AiAgentInvocationLogRepository invocationLogRepository,
            ConversationAiDraftRepository draftRepository,
            WorkflowRunRepository workflowRunRepository,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowEngineService workflowEngineService,
            AiAgentContextAssembler contextAssembler,
            LlmClientFactory llmClientFactory,
            MessagingService messagingService,
            ConversationRepository conversationRepository,
            ApplicationEventPublisher eventPublisher,
            AiAgentInvocationSlotClaimer slotClaimer,
            ContactCustomFieldWriter contactCustomFieldWriter) {
        this.configurationRepository = configurationRepository;
        this.invocationLogRepository = invocationLogRepository;
        this.draftRepository = draftRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowEngineService = workflowEngineService;
        this.contextAssembler = contextAssembler;
        this.llmClientFactory = llmClientFactory;
        this.messagingService = messagingService;
        this.conversationRepository = conversationRepository;
        this.eventPublisher = eventPublisher;
        this.slotClaimer = slotClaimer;
        this.contactCustomFieldWriter = contactCustomFieldWriter;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invoke(Conversation conversation, Message triggeringMessage) {
        UUID workspaceId = conversation.getWorkspace().getId();
        UUID conversationId = conversation.getId();

        Optional<AiAgentConfiguration> agentConfigurationOptional =
                configurationRepository.findByWorkspaceIdAndEnabledTrue(workspaceId);

        if (agentConfigurationOptional.isEmpty()) {
            return;
        }

        AiAgentConfiguration configuration = agentConfigurationOptional.get();

        // Close any prior CLARIFYING turn so the unique constraint slot is freed before claiming
        invocationLogRepository
                .findByConversationIdAndStatus(conversationId, AiAgentInvocationStatus.CLARIFYING)
                .ifPresent(
                        prior -> {
                            prior.setStatus(AiAgentInvocationStatus.SENT);
                            prior.setFinishedAt(Instant.now());
                            invocationLogRepository.saveAndFlush(prior);
                        });

        // Claim via a nested REQUIRES_NEW transaction so a constraint violation on
        // uq_ai_invocation_conversation_active only rolls back the inner transaction,
        // leaving this one clean.
        Optional<AiAgentInvocationLog> claimed =
                slotClaimer.tryClaim(conversation, triggeringMessage);

        if (claimed.isEmpty()) {
            log.debug(
                    "AI agent invocation skipped for conversation={} — another invocation is already active",
                    conversationId);

            return;
        }

        AiAgentInvocationLog invocationLog = claimed.get();

        conversation.setLockedByAiAgent(true);
        conversationRepository.save(conversation);

        // Re-check for an active workflow run that may have been committed between our first check
        // and the log INSERT
        boolean workflowActive =
                !workflowRunRepository
                                .findForConversationWithStatus(
                                        conversationId, WorkflowRunStatus.RUNNING)
                                .isEmpty()
                        || !workflowRunRepository
                                .findForConversationWithStatus(
                                        conversationId, WorkflowRunStatus.WAITING)
                                .isEmpty();

        if (workflowActive) {
            invocationLogRepository.delete(invocationLog);
            log.debug(
                    "AI agent invocation aborted for conversation={} — workflow claimed it first",
                    conversationId);

            return;
        }

        try {
            runPipeline(configuration, conversation, triggeringMessage, invocationLog);
        } catch (Exception e) {
            log.error(
                    "AI agent pipeline failed for conversation={}: {}",
                    conversationId,
                    e.getMessage(),
                    e);
            invocationLog.setStatus(AiAgentInvocationStatus.FAILED);
            invocationLog.setEscalationReason("Internal error: " + e.getMessage());
            invocationLog.setFinishedAt(Instant.now());
            invocationLogRepository.save(invocationLog);
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void runPipeline(
            AiAgentConfiguration configuration,
            Conversation conversation,
            Message triggeringMessage,
            AiAgentInvocationLog invocationLog) {

        UUID workspaceId = conversation.getWorkspace().getId();
        UUID conversationId = conversation.getId();
        String messageText = triggeringMessage.getText() != null ? triggeringMessage.getText() : "";

        // Deterministic escalation keyword check before any LLM call
        for (String keyword : configuration.getEscalationKeywords()) {
            if (messageText.toLowerCase().contains(keyword.toLowerCase())) {
                String reason = "Escalation keyword matched: \"" + keyword + "\"";
                finalise(invocationLog, AiAgentInvocationStatus.ESCALATED, reason);
                broadcastEscalation(workspaceId, conversationId, reason);

                return;
            }
        }

        // Assemble context and call the LLM
        AgentLlmRequest request = contextAssembler.assemble(configuration, conversation);
        AgentLlmResponse response = llmClientFactory.getActiveClient().complete(request);

        invocationLog.setOutputSnapshot(
                Map.of(
                        "reply", response.reply(),
                        "confidence", response.confidence() != null ? response.confidence() : "",
                        "escalate", response.escalate(),
                        "needsClarification", response.needsClarification(),
                        "suggestedActions", response.suggestedActions(),
                        "extractedData", response.extractedData()));

        // Decision flow
        boolean draftOnly = configuration.getAutonomyCeiling() == AutonomyCeiling.DRAFT_ONLY;

        if (response.escalate()) {
            String reason = "LLM requested escalation";
            contactCustomFieldWriter.apply(
                    conversation, response.extractedData(), configuration.getExtractionFields());
            finalise(invocationLog, AiAgentInvocationStatus.ESCALATED, reason);
            broadcastEscalation(workspaceId, conversationId, reason);

            return;
        }

        Optional<String> workflowAction =
                response.suggestedActions().stream()
                        .filter(action -> action.startsWith(WORKFLOW_ACTION_PREFIX))
                        .findFirst();

        if (workflowAction.isPresent() && !draftOnly) {
            String rawId = workflowAction.get().substring(WORKFLOW_ACTION_PREFIX.length());
            contactCustomFieldWriter.apply(
                    conversation, response.extractedData(), configuration.getExtractionFields());
            triggerWorkflow(
                    rawId,
                    workspaceId,
                    conversation,
                    triggeringMessage,
                    response,
                    configuration.getExtractionFields(),
                    invocationLog);

            return;
        }

        boolean shouldDraft =
                draftOnly || "low".equals(response.confidence()) || response.needsClarification();

        // Drafts defer writing to the contact until a human approves — extraction from an
        // unreviewed draft may be wrong, and this record persists past a single workflow run.
        if (shouldDraft) {
            saveDraft(conversation, response, invocationLog);
            broadcastDraftCreated(workspaceId, conversationId);
            finalise(invocationLog, AiAgentInvocationStatus.DRAFTED, null);

            return;
        }

        contactCustomFieldWriter.apply(
                conversation, response.extractedData(), configuration.getExtractionFields());
        sendOutbound(workspaceId, conversationId, response.reply());
        finalise(invocationLog, AiAgentInvocationStatus.SENT, null);
    }

    private void sendOutbound(UUID workspaceId, UUID conversationId, String text) {
        messagingService.createMessage(
                workspaceId,
                conversationId,
                new CreateMessageRequest(
                        MessageDirection.OUTBOUND, MessageSenderType.SYSTEM, text, null, Map.of()),
                null);
    }

    private void triggerWorkflow(
            String rawWorkflowId,
            UUID workspaceId,
            Conversation conversation,
            Message triggeringMessage,
            AgentLlmResponse response,
            List<ExtractionField> extractionFields,
            AiAgentInvocationLog invocationLog) {
        UUID workflowId;

        try {
            workflowId = UUID.fromString(rawWorkflowId);
        } catch (IllegalArgumentException e) {
            log.warn(
                    "AI agent returned invalid workflow ID '{}' for conversation={}",
                    rawWorkflowId,
                    conversation.getId());
            finalise(
                    invocationLog,
                    AiAgentInvocationStatus.FAILED,
                    "Invalid workflow ID: " + rawWorkflowId);

            return;
        }

        Optional<WorkflowDefinition> workflowDefinitionOptional =
                workflowDefinitionRepository.findInWorkspace(workflowId, workspaceId);

        if (workflowDefinitionOptional.isEmpty()) {
            log.warn(
                    "AI agent referenced unknown workflow={} in workspace={}",
                    workflowId,
                    workspaceId);
            finalise(
                    invocationLog,
                    AiAgentInvocationStatus.FAILED,
                    "Workflow not found: " + workflowId);

            return;
        }

        Map<String, String> agentContext =
                AgentWorkflowContext.build(
                        response.reply(),
                        response.confidence(),
                        response.extractedData(),
                        extractionFields);

        workflowEngineService.executeWorkflow(
                workflowDefinitionOptional.get(), conversation, triggeringMessage, agentContext);

        finalise(invocationLog, AiAgentInvocationStatus.SENT, null);
    }

    private void saveDraft(
            Conversation conversation,
            AgentLlmResponse response,
            AiAgentInvocationLog invocationLog) {
        // Replace any existing draft (one draft per conversation at a time)
        draftRepository
                .findByConversationId(conversation.getId())
                .ifPresent(draftRepository::delete);

        ConversationAiDraft draft = new ConversationAiDraft();
        draft.setWorkspace(conversation.getWorkspace());
        draft.setConversation(conversation);
        draft.setInvocationLogId(invocationLog.getId());
        draft.setProposedReply(response.reply());
        draft.setSuggestedActions(response.suggestedActions());
        draft.setExtractedData(response.extractedData());
        draftRepository.save(draft);
    }

    private void broadcastEscalation(UUID workspaceId, UUID conversationId, String reason) {
        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        workspaceId,
                        "ai.escalated",
                        Map.of("conversationId", conversationId.toString(), "reason", reason)));
    }

    private void broadcastDraftCreated(UUID workspaceId, UUID conversationId) {
        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        workspaceId,
                        "ai.draft.created",
                        Map.of("conversationId", conversationId.toString())));
    }

    private void finalise(
            AiAgentInvocationLog invocationLog,
            AiAgentInvocationStatus status,
            String escalationReason) {
        invocationLog.setStatus(status);
        invocationLog.setFinishedAt(
                status == AiAgentInvocationStatus.CLARIFYING ? null : Instant.now());

        if (escalationReason != null) {
            invocationLog.setEscalationReason(escalationReason);
        }

        invocationLogRepository.save(invocationLog);

        if (status != AiAgentInvocationStatus.CLARIFYING) {
            Conversation conversation = invocationLog.getConversation();
            conversation.setLockedByAiAgent(false);
            conversationRepository.save(conversation);
        }
    }
}
