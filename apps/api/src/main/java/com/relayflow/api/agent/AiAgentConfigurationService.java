package com.relayflow.api.agent;

import com.relayflow.api.agent.domain.AiAgentConfiguration;
import com.relayflow.api.agent.domain.ConversationAiDraft;
import com.relayflow.api.agent.domain.ExtractionField;
import com.relayflow.api.agent.dto.AiAgentConfigurationResponse;
import com.relayflow.api.agent.dto.ChannelAiAgentAssignmentResponse;
import com.relayflow.api.agent.dto.ConversationAiDraftResponse;
import com.relayflow.api.agent.dto.UpdateAiAgentConfigurationRequest;
import com.relayflow.api.agent.repository.AiAgentConfigurationRepository;
import com.relayflow.api.agent.repository.ConversationAiDraftRepository;
import com.relayflow.api.channel.domain.ChannelAccount;
import com.relayflow.api.channel.repository.ChannelAccountRepository;
import com.relayflow.api.common.ResourceNotFoundException;
import com.relayflow.api.messaging.MessagingService;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.dto.CreateMessageRequest;
import com.relayflow.api.messaging.dto.MessageResponse;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.workflow.domain.WorkflowDefinition;
import com.relayflow.api.workflow.engine.WorkflowEngineService;
import com.relayflow.api.workflow.repository.WorkflowDefinitionRepository;
import com.relayflow.api.workspace.domain.Workspace;
import com.relayflow.api.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAgentConfigurationService {

    private final AiAgentConfigurationRepository configurationRepository;

    private final ConversationAiDraftRepository draftRepository;

    private final WorkspaceRepository workspaceRepository;

    private final ChannelAccountRepository channelAccountRepository;

    private final ConversationRepository conversationRepository;

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    private final WorkflowEngineService workflowEngineService;

    private final MessagingService messagingService;

    public AiAgentConfigurationService(
            AiAgentConfigurationRepository configurationRepository,
            ConversationAiDraftRepository draftRepository,
            WorkspaceRepository workspaceRepository,
            ChannelAccountRepository channelAccountRepository,
            ConversationRepository conversationRepository,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowEngineService workflowEngineService,
            MessagingService messagingService) {
        this.configurationRepository = configurationRepository;
        this.draftRepository = draftRepository;
        this.workspaceRepository = workspaceRepository;
        this.channelAccountRepository = channelAccountRepository;
        this.conversationRepository = conversationRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowEngineService = workflowEngineService;
        this.messagingService = messagingService;
    }

    @Transactional(readOnly = true)
    public List<AiAgentConfigurationResponse> listConfigurations(UUID workspaceId) {
        return configurationRepository.findAllByWorkspaceId(workspaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AiAgentConfigurationResponse createConfiguration(
            UUID workspaceId, UpdateAiAgentConfigurationRequest request) {
        Workspace workspace =
                workspaceRepository
                        .findById(workspaceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        boolean isFirstConfiguration =
                configurationRepository.findAllByWorkspaceId(workspaceId).isEmpty();

        AiAgentConfiguration configuration = new AiAgentConfiguration();
        configuration.setWorkspace(workspace);
        configuration.setDefaultConfig(isFirstConfiguration);

        return toResponse(applyUpdate(configuration, request));
    }

    @Transactional
    public AiAgentConfigurationResponse updateConfiguration(
            UUID workspaceId, UUID configurationId, UpdateAiAgentConfigurationRequest request) {
        AiAgentConfiguration configuration = getOrThrow(workspaceId, configurationId);

        return toResponse(applyUpdate(configuration, request));
    }

    @Transactional
    public void deleteConfiguration(UUID workspaceId, UUID configurationId) {
        AiAgentConfiguration configuration = getOrThrow(workspaceId, configurationId);
        boolean wasDefault = configuration.isDefaultConfig();

        configurationRepository.delete(configuration);

        if (wasDefault) {
            configurationRepository.findAllByWorkspaceId(workspaceId).stream()
                    .findFirst()
                    .ifPresent(
                            next -> {
                                next.setDefaultConfig(true);
                                configurationRepository.save(next);
                            });
        }
    }

    @Transactional
    public AiAgentConfigurationResponse setDefaultConfiguration(
            UUID workspaceId, UUID configurationId) {
        AiAgentConfiguration configuration = getOrThrow(workspaceId, configurationId);

        configurationRepository.clearDefault(workspaceId);
        configuration.setDefaultConfig(true);

        return toResponse(configurationRepository.save(configuration));
    }

    @Transactional(readOnly = true)
    public ChannelAiAgentAssignmentResponse getChannelAssignment(
            UUID workspaceId, UUID channelAccountId) {
        ChannelAccount channelAccount = getChannelOrThrow(workspaceId, channelAccountId);
        AiAgentConfiguration assigned = channelAccount.getAiAgentConfiguration();

        return new ChannelAiAgentAssignmentResponse(assigned != null ? assigned.getId() : null);
    }

    @Transactional
    public ChannelAiAgentAssignmentResponse assignConfigurationToChannel(
            UUID workspaceId, UUID channelAccountId, UUID configurationId) {
        ChannelAccount channelAccount = getChannelOrThrow(workspaceId, channelAccountId);

        if (configurationId == null) {
            channelAccount.setAiAgentConfiguration(null);
        } else {
            channelAccount.setAiAgentConfiguration(getOrThrow(workspaceId, configurationId));
        }

        channelAccountRepository.save(channelAccount);

        return new ChannelAiAgentAssignmentResponse(configurationId);
    }

    @Transactional(readOnly = true)
    public ConversationAiDraftResponse getDraft(UUID workspaceId, UUID conversationId) {
        return draftRepository
                .findByConversationId(conversationId)
                .filter(draft -> draft.getWorkspace().getId().equals(workspaceId))
                .map(this::toDraftResponse)
                .orElseThrow(() -> new ResourceNotFoundException("AI draft not found"));
    }

    @Transactional
    public MessageResponse sendDraft(UUID workspaceId, UUID conversationId, String editedReply) {
        ConversationAiDraft draft =
                draftRepository
                        .findByConversationId(conversationId)
                        .filter(d -> d.getWorkspace().getId().equals(workspaceId))
                        .orElseThrow(() -> new ResourceNotFoundException("AI draft not found"));

        String text =
                (editedReply != null && !editedReply.isBlank())
                        ? editedReply.trim()
                        : draft.getProposedReply();

        MessageResponse sent =
                messagingService.createMessage(
                        workspaceId,
                        conversationId,
                        new CreateMessageRequest(
                                MessageDirection.OUTBOUND,
                                MessageSenderType.SYSTEM,
                                text,
                                null,
                                Map.of()),
                        null);

        draftRepository.delete(draft);

        return sent;
    }

    @Transactional
    public void discardDraft(UUID workspaceId, UUID conversationId) {
        draftRepository
                .findByConversationId(conversationId)
                .filter(draft -> draft.getWorkspace().getId().equals(workspaceId))
                .ifPresent(draftRepository::delete);
    }

    @Transactional
    public void triggerWorkflowFromDraft(UUID workspaceId, UUID conversationId, UUID workflowId) {
        ConversationAiDraft draft =
                draftRepository
                        .findByConversationId(conversationId)
                        .filter(d -> d.getWorkspace().getId().equals(workspaceId))
                        .orElseThrow(() -> new ResourceNotFoundException("AI draft not found"));

        String expectedAction = "trigger_workflow:" + workflowId;
        boolean actionPresent = draft.getSuggestedActions().contains(expectedAction);

        if (!actionPresent) {
            throw new ResourceNotFoundException(
                    "Workflow action not found in draft suggested actions");
        }

        Conversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        WorkflowDefinition workflowDefinition =
                workflowDefinitionRepository
                        .findInWorkspace(workflowId, workspaceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));

        List<ExtractionField> extractionFields =
                extractionFieldsFor(workspaceId, conversation.getChannelAccount().getId());

        draftRepository.delete(draft);

        // Triggered by a human approving the draft — no originating message, no confidence
        // score (that lived on the invocation's LLM response, not the persisted draft).
        Map<String, String> agentContext =
                AgentWorkflowContext.build(
                        draft.getProposedReply(), null, draft.getExtractedData(), extractionFields);

        workflowEngineService.executeWorkflow(workflowDefinition, conversation, null, agentContext);
    }

    // ── Package-private (used by the invocation pipeline) ───────────────────────

    /**
     * The channel's own assigned config if enabled, else the workspace's enabled default. An
     * explicit channel assignment is authoritative — a disabled explicit assignment does not fall
     * back to the default.
     */
    @Transactional(readOnly = true)
    Optional<AiAgentConfiguration> resolveEnabledConfiguration(
            UUID workspaceId, UUID channelAccountId) {
        Optional<AiAgentConfiguration> assigned =
                channelAccountRepository.findAiAgentConfiguration(channelAccountId);

        if (assigned.isPresent()) {
            return assigned.filter(AiAgentConfiguration::isEnabled);
        }

        return configurationRepository
                .findByWorkspaceIdAndDefaultConfigTrue(workspaceId)
                .filter(AiAgentConfiguration::isEnabled);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private List<ExtractionField> extractionFieldsFor(UUID workspaceId, UUID channelAccountId) {
        Optional<AiAgentConfiguration> assigned =
                channelAccountRepository.findAiAgentConfiguration(channelAccountId);

        return assigned.or(
                        () ->
                                configurationRepository.findByWorkspaceIdAndDefaultConfigTrue(
                                        workspaceId))
                .map(AiAgentConfiguration::getExtractionFields)
                .orElse(List.of());
    }

    private AiAgentConfiguration applyUpdate(
            AiAgentConfiguration configuration, UpdateAiAgentConfigurationRequest request) {
        if (request.name() != null && !request.name().isBlank()) {
            configuration.setName(request.name().trim());
        }
        configuration.setEnabled(request.enabled());
        configuration.setAutonomyCeiling(request.autonomyCeiling());
        configuration.setLlmProvider(request.llmProvider());
        configuration.setInstructions(request.instructions());
        configuration.setKnowledgeBase(request.knowledgeBase());
        configuration.setEscalationKeywords(request.escalationKeywords());
        configuration.setWorkflowMappings(request.workflowMappings());
        configuration.setExtractionFields(request.extractionFields());

        return configurationRepository.save(configuration);
    }

    private AiAgentConfiguration getOrThrow(UUID workspaceId, UUID configurationId) {
        return configurationRepository
                .findByIdAndWorkspaceId(configurationId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("AI agent config not found"));
    }

    private ChannelAccount getChannelOrThrow(UUID workspaceId, UUID channelAccountId) {
        return channelAccountRepository
                .findInWorkspace(channelAccountId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel account not found"));
    }

    private AiAgentConfigurationResponse toResponse(AiAgentConfiguration configuration) {
        return new AiAgentConfigurationResponse(
                configuration.getId(),
                configuration.getWorkspace().getId(),
                configuration.getName(),
                configuration.isDefaultConfig(),
                configuration.isEnabled(),
                configuration.getAutonomyCeiling(),
                configuration.getLlmProvider(),
                configuration.getInstructions(),
                configuration.getKnowledgeBase(),
                configuration.getEscalationKeywords(),
                configuration.getWorkflowMappings(),
                configuration.getExtractionFields(),
                configuration.getCreatedAt(),
                configuration.getUpdatedAt());
    }

    private ConversationAiDraftResponse toDraftResponse(ConversationAiDraft draft) {
        return new ConversationAiDraftResponse(
                draft.getId(),
                draft.getWorkspace().getId(),
                draft.getConversation().getId(),
                draft.getInvocationLogId(),
                draft.getProposedReply(),
                draft.getSuggestedActions(),
                draft.getExtractedData(),
                draft.getCreatedAt());
    }
}
