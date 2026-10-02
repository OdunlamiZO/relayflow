package com.relayflow.api.agent;

import com.relayflow.api.agent.domain.AiAgentConfiguration;
import com.relayflow.api.agent.domain.AiAgentInvocationLog;
import com.relayflow.api.agent.domain.AiAgentInvocationStatus;
import com.relayflow.api.agent.domain.AutonomyCeiling;
import com.relayflow.api.agent.domain.ConversationAiDraft;
import com.relayflow.api.agent.domain.ExtractionField;
import com.relayflow.api.agent.domain.ExtractionValidation;
import com.relayflow.api.agent.domain.RejectedExtraction;
import com.relayflow.api.agent.domain.WorkflowMapping;
import com.relayflow.api.agent.llm.AgentLlmRequest;
import com.relayflow.api.agent.llm.AgentLlmResponse;
import com.relayflow.api.agent.llm.LlmClientFactory;
import com.relayflow.api.agent.llm.LlmMessage;
import com.relayflow.api.agent.repository.AiAgentInvocationLogRepository;
import com.relayflow.api.agent.repository.ConversationAiDraftRepository;
import com.relayflow.api.messaging.MessagingService;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.EscalationResolution;
import com.relayflow.api.messaging.domain.EscalationType;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.dto.CreateMessageRequest;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.sse.SseBroadcastEvent;
import com.relayflow.api.sse.SseEventType;
import com.relayflow.api.workflow.domain.WorkflowDefinition;
import com.relayflow.api.workflow.domain.WorkflowRunStatus;
import com.relayflow.api.workflow.engine.WorkflowEngineService;
import com.relayflow.api.workflow.repository.WorkflowDefinitionRepository;
import com.relayflow.api.workflow.repository.WorkflowRunRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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

    private static final String CONVERSION_PROMPT =
            "You convert a customer's answer into the format a field requires, as described by"
                    + " the reason it was rejected. Put only the converted value in the reply, or"
                    + " leave the reply empty if it can't be converted without guessing.";

    private final AiAgentConfigurationService aiAgentConfigurationService;

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

    private final ExtractedDataValidator extractedDataValidator;

    private final PublishedWorkflowMappings publishedWorkflowMappings;

    public AiAgentInvocationService(
            AiAgentConfigurationService aiAgentConfigurationService,
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
            ContactCustomFieldWriter contactCustomFieldWriter,
            ExtractedDataValidator extractedDataValidator,
            PublishedWorkflowMappings publishedWorkflowMappings) {
        this.aiAgentConfigurationService = aiAgentConfigurationService;
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
        this.extractedDataValidator = extractedDataValidator;
        this.publishedWorkflowMappings = publishedWorkflowMappings;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invoke(Conversation staleConversation, Message triggeringMessage) {
        UUID conversationId = staleConversation.getId();

        // staleConversation's lazy associations belong to an already-closed session.
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);

        if (conversation == null) {
            return;
        }

        UUID workspaceId = conversation.getWorkspace().getId();
        UUID channelAccountId = conversation.getChannelAccount().getId();

        Optional<AiAgentConfiguration> agentConfigurationOptional =
                aiAgentConfigurationService.resolveEnabledConfiguration(
                        workspaceId, channelAccountId);

        if (agentConfigurationOptional.isEmpty()) {
            return;
        }

        AiAgentConfiguration configuration = agentConfigurationOptional.get();

        // Close any prior CLARIFYING turn so the unique constraint slot is freed before claiming
        invocationLogRepository
                .findForConversationWithStatus(conversationId, AiAgentInvocationStatus.CLARIFYING)
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
            broadcastEscalation(conversation, EscalationType.INTERNAL_ERROR, "Internal error");
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
                broadcastEscalation(conversation, EscalationType.KEYWORD_MATCHED, reason);

                return;
            }
        }

        // Assemble context and call the LLM
        AgentLlmRequest request = contextAssembler.assemble(configuration, conversation);
        AgentLlmResponse response =
                llmClientFactory.getClient(configuration.getLlmProvider()).complete(request);

        if (response.failed()) {
            String reason = "LLM call failed";
            finalise(invocationLog, AiAgentInvocationStatus.ESCALATED, reason);
            broadcastEscalation(conversation, EscalationType.LLM_FAILED, reason);

            return;
        }

        ExtractionValidation validation =
                convertRejectedValues(
                        configuration,
                        request,
                        workspaceId,
                        extractedDataValidator.validate(
                                workspaceId,
                                response.extractedData(),
                                configuration.getExtractionFields()));

        invocationLog.setOutputSnapshot(
                Map.of(
                        "reply", response.reply(),
                        "confidence", response.confidence() != null ? response.confidence() : "",
                        "escalate", response.escalate(),
                        "needsClarification", response.needsClarification(),
                        "suggestedActions", response.suggestedActions(),
                        "extractedData", response.extractedData(),
                        "rejectedExtractions",
                                validation.rejections().stream()
                                        .map(RejectedExtraction::toSnapshot)
                                        .toList()));

        contactCustomFieldWriter.apply(
                conversation, validation.acceptedData(), configuration.getExtractionFields());

        Map<String, String> accumulatedExtractedData =
                mergeWithPendingDraft(conversation, validation.acceptedData());

        List<String> suggestedActions =
                sanitizeSuggestedActions(
                        response.suggestedActions(),
                        publishedWorkflowMappings.filter(
                                workspaceId, configuration.getWorkflowMappings()));

        // A workflow shouldn't start while a value it may need is still being corrected.
        if (validation.hasRejections()) {
            suggestedActions =
                    suggestedActions.stream()
                            .filter(action -> !action.startsWith(WORKFLOW_ACTION_PREFIX))
                            .toList();
        }

        // Decision flow
        boolean draftOnly = configuration.getAutonomyCeiling() == AutonomyCeiling.DRAFT_ONLY;

        if (response.escalate()) {
            String reason = "LLM requested escalation";
            finalise(invocationLog, AiAgentInvocationStatus.ESCALATED, reason);
            broadcastEscalation(conversation, EscalationType.AI_REQUESTED, reason);

            return;
        }

        resolveTemporaryEscalation(conversation);

        if (validation.hasRejections()) {
            response =
                    withReply(
                            response,
                            correctionReply(configuration, request, response, validation));
        }

        Optional<String> workflowAction =
                suggestedActions.stream()
                        .filter(action -> action.startsWith(WORKFLOW_ACTION_PREFIX))
                        .findFirst();

        if (workflowAction.isPresent() && !draftOnly) {
            String rawId = workflowAction.get().substring(WORKFLOW_ACTION_PREFIX.length());
            triggerWorkflow(
                    rawId,
                    workspaceId,
                    conversation,
                    triggeringMessage,
                    response,
                    accumulatedExtractedData,
                    configuration.getExtractionFields(),
                    invocationLog);

            return;
        }

        boolean shouldDraft =
                draftOnly || "low".equals(response.confidence()) || response.needsClarification();

        if (shouldDraft) {
            saveDraft(
                    conversation,
                    response,
                    suggestedActions,
                    accumulatedExtractedData,
                    invocationLog);
            broadcastDraftCreated(workspaceId, conversationId);
            finalise(invocationLog, AiAgentInvocationStatus.DRAFTED, null);

            return;
        }

        sendOutbound(workspaceId, conversationId, response.reply());
        finalise(invocationLog, AiAgentInvocationStatus.SENT, null);
    }

    private ExtractionValidation convertRejectedValues(
            AiAgentConfiguration configuration,
            AgentLlmRequest request,
            UUID workspaceId,
            ExtractionValidation validation) {
        Map<String, String> converted = new LinkedHashMap<>();

        for (RejectedExtraction rejection : validation.rejections()) {
            AgentLlmResponse conversion =
                    llmClientFactory
                            .getClient(configuration.getLlmProvider())
                            .complete(
                                    new AgentLlmRequest(
                                            CONVERSION_PROMPT,
                                            List.of(LlmMessage.user(rejection.conversionRequest())),
                                            request.model(),
                                            List.of()));
            String value = conversion.reply().trim();

            if (!conversion.failed() && !value.isEmpty() && !value.equals(rejection.value())) {
                converted.put(rejection.key(), value);
            }
        }

        if (converted.isEmpty()) {
            return validation;
        }

        ExtractionValidation recheck =
                extractedDataValidator.validate(
                        workspaceId, converted, configuration.getExtractionFields());

        Map<String, String> acceptedData = new LinkedHashMap<>(validation.acceptedData());
        acceptedData.putAll(recheck.acceptedData());

        List<RejectedExtraction> rejections =
                validation.rejections().stream()
                        .filter(rejection -> !recheck.acceptedData().containsKey(rejection.key()))
                        .toList();

        return new ExtractionValidation(acceptedData, rejections);
    }

    private String correctionReply(
            AiAgentConfiguration configuration,
            AgentLlmRequest request,
            AgentLlmResponse response,
            ExtractionValidation validation) {
        // No extraction fields, so the rewrite can't bring a rejected value back.
        AgentLlmRequest correctionRequest =
                new AgentLlmRequest(
                        request.systemPrompt() + validation.correctionInstruction(response.reply()),
                        request.messages(),
                        request.model(),
                        List.of());
        AgentLlmResponse correction =
                llmClientFactory
                        .getClient(configuration.getLlmProvider())
                        .complete(correctionRequest);

        if (correction.failed() || correction.reply().isBlank()) {
            return validation.fallbackReply();
        }

        return correction.reply();
    }

    private AgentLlmResponse withReply(AgentLlmResponse response, String reply) {
        return new AgentLlmResponse(
                reply,
                response.confidence(),
                response.suggestedActions(),
                response.escalate(),
                response.needsClarification(),
                response.extractedData(),
                response.failed());
    }

    private Map<String, String> mergeWithPendingDraft(
            Conversation conversation, Map<String, String> newData) {
        Optional<ConversationAiDraft> pending =
                draftRepository.findByConversation(conversation.getId());

        if (pending.isPresent()
                && pending.get().getCreatedAt().isBefore(conversation.getSessionStartedAt())) {
            draftRepository.delete(pending.get());
            pending = Optional.empty();
        }

        Map<String, String> merged =
                pending.map(draft -> new LinkedHashMap<>(draft.getExtractedData()))
                        .orElseGet(LinkedHashMap::new);
        merged.putAll(newData);

        return merged;
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
            Map<String, String> accumulatedExtractedData,
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
                        accumulatedExtractedData,
                        extractionFields);

        draftRepository.findByConversation(conversation.getId()).ifPresent(draftRepository::delete);

        workflowEngineService.executeWorkflow(
                workflowDefinitionOptional.get(), conversation, triggeringMessage, agentContext);

        finalise(invocationLog, AiAgentInvocationStatus.SENT, null);
    }

    private void saveDraft(
            Conversation conversation,
            AgentLlmResponse response,
            List<String> suggestedActions,
            Map<String, String> accumulatedExtractedData,
            AiAgentInvocationLog invocationLog) {
        ConversationAiDraft draft =
                draftRepository
                        .findByConversation(conversation.getId())
                        .orElseGet(ConversationAiDraft::new);

        draft.setWorkspace(conversation.getWorkspace());
        draft.setConversation(conversation);
        draft.setInvocationLogId(invocationLog.getId());
        draft.setProposedReply(response.reply());
        draft.setSuggestedActions(suggestedActions);
        draft.setExtractedData(accumulatedExtractedData);
        draftRepository.save(draft);
    }

    private List<String> sanitizeSuggestedActions(
            List<String> actions, List<WorkflowMapping> workflowMappings) {
        Set<String> configuredWorkflowIds =
                workflowMappings.stream()
                        .map(mapping -> mapping.workflowId().toString())
                        .collect(Collectors.toSet());

        return actions.stream()
                .filter(
                        action ->
                                !action.startsWith(WORKFLOW_ACTION_PREFIX)
                                        || configuredWorkflowIds.contains(
                                                action.substring(WORKFLOW_ACTION_PREFIX.length())))
                .toList();
    }

    private void broadcastEscalation(
            Conversation conversation, EscalationType escalationType, String reason) {
        conversation.setEscalatedAt(Instant.now());
        conversation.setEscalationReason(reason);
        conversation.setEscalationType(escalationType);
        conversationRepository.save(conversation);

        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        conversation.getWorkspace().getId(),
                        SseEventType.AI_ESCALATED,
                        Map.of(
                                "conversationId",
                                conversation.getId().toString(),
                                "reason",
                                reason)));
    }

    private void resolveTemporaryEscalation(Conversation conversation) {
        EscalationType escalationType = conversation.getEscalationType();

        if (conversation.getEscalatedAt() == null
                || escalationType == null
                || escalationType.getResolution() != EscalationResolution.AI_RECOVERY) {
            return;
        }

        conversation.setEscalatedAt(null);
        conversation.setEscalationReason(null);
        conversation.setEscalationType(null);
        conversationRepository.save(conversation);

        UUID workspaceId = conversation.getWorkspace().getId();

        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        workspaceId,
                        SseEventType.CONVERSATION_UPDATED,
                        Map.of(
                                "workspaceId", workspaceId.toString(),
                                "conversationId", conversation.getId().toString())));
    }

    private void broadcastDraftCreated(UUID workspaceId, UUID conversationId) {
        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        workspaceId,
                        SseEventType.AI_DRAFT_CREATED,
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
