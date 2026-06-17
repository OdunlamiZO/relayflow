package com.relayflow.api.agent;

import com.relayflow.api.agent.domain.AiAgentConfiguration;
import com.relayflow.api.agent.domain.ConversationAiDraft;
import com.relayflow.api.agent.dto.AiAgentConfigurationResponse;
import com.relayflow.api.agent.dto.ConversationAiDraftResponse;
import com.relayflow.api.agent.dto.UpdateAiAgentConfigurationRequest;
import com.relayflow.api.agent.repository.AiAgentConfigurationRepository;
import com.relayflow.api.agent.repository.ConversationAiDraftRepository;
import com.relayflow.api.messaging.MessagingService;
import com.relayflow.api.messaging.ResourceNotFoundException;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.domain.Workspace;
import com.relayflow.api.messaging.dto.CreateMessageRequest;
import com.relayflow.api.messaging.dto.MessageResponse;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.messaging.repository.WorkspaceRepository;
import com.relayflow.api.workflow.domain.WorkflowDefinition;
import com.relayflow.api.workflow.engine.WorkflowEngineService;
import com.relayflow.api.workflow.repository.WorkflowDefinitionRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAgentConfigurationService {

    private final AiAgentConfigurationRepository configurationRepository;

    private final ConversationAiDraftRepository draftRepository;

    private final WorkspaceRepository workspaceRepository;

    private final ConversationRepository conversationRepository;

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    private final WorkflowEngineService workflowEngineService;

    private final MessagingService messagingService;

    public AiAgentConfigurationService(
            AiAgentConfigurationRepository configurationRepository,
            ConversationAiDraftRepository draftRepository,
            WorkspaceRepository workspaceRepository,
            ConversationRepository conversationRepository,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowEngineService workflowEngineService,
            MessagingService messagingService) {
        this.configurationRepository = configurationRepository;
        this.draftRepository = draftRepository;
        this.workspaceRepository = workspaceRepository;
        this.conversationRepository = conversationRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowEngineService = workflowEngineService;
        this.messagingService = messagingService;
    }

    @Transactional
    public AiAgentConfigurationResponse getOrCreateConfiguration(UUID workspaceId) {
        return configurationRepository
                .findByWorkspaceId(workspaceId)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(createDefaultConfiguration(workspaceId)));
    }

    @Transactional
    public AiAgentConfigurationResponse updateConfiguration(
            UUID workspaceId, UpdateAiAgentConfigurationRequest request) {
        AiAgentConfiguration configuration =
                configurationRepository
                        .findByWorkspaceId(workspaceId)
                        .orElseGet(() -> createDefaultConfiguration(workspaceId));

        if (request.name() != null && !request.name().isBlank()) {
            configuration.setName(request.name().trim());
        }
        configuration.setEnabled(request.enabled());
        configuration.setAutonomyCeiling(request.autonomyCeiling());
        configuration.setInstructions(request.instructions());
        configuration.setKnowledgeBase(request.knowledgeBase());
        configuration.setEscalationKeywords(request.escalationKeywords());
        configuration.setWorkflowMappings(request.workflowMappings());

        return toResponse(configurationRepository.save(configuration));
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
    public MessageResponse sendDraft(UUID workspaceId, UUID conversationId) {
        ConversationAiDraft draft =
                draftRepository
                        .findByConversationId(conversationId)
                        .filter(d -> d.getWorkspace().getId().equals(workspaceId))
                        .orElseThrow(() -> new ResourceNotFoundException("AI draft not found"));

        MessageResponse sent =
                messagingService.createMessage(
                        workspaceId,
                        conversationId,
                        new CreateMessageRequest(
                                MessageDirection.OUTBOUND,
                                MessageSenderType.SYSTEM,
                                draft.getProposedReply(),
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

        draftRepository.delete(draft);

        // Triggered by a human approving the draft — no originating message to pass along.
        workflowEngineService.executeWorkflow(workflowDefinition, conversation, null);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private AiAgentConfiguration createDefaultConfiguration(UUID workspaceId) {
        Workspace workspace =
                workspaceRepository
                        .findById(workspaceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        AiAgentConfiguration configuration = new AiAgentConfiguration();
        configuration.setWorkspace(workspace);

        return configurationRepository.save(configuration);
    }

    private AiAgentConfigurationResponse toResponse(AiAgentConfiguration configuration) {
        return new AiAgentConfigurationResponse(
                configuration.getId(),
                configuration.getWorkspace().getId(),
                configuration.getName(),
                configuration.isEnabled(),
                configuration.getAutonomyCeiling(),
                configuration.getInstructions(),
                configuration.getKnowledgeBase(),
                configuration.getEscalationKeywords(),
                configuration.getWorkflowMappings(),
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
                draft.getCreatedAt());
    }
}
