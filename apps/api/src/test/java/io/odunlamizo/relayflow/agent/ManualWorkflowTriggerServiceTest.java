package io.odunlamizo.relayflow.agent;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.odunlamizo.relayflow.agent.domain.AiAgentConfiguration;
import io.odunlamizo.relayflow.agent.domain.AiAgentInvocationLog;
import io.odunlamizo.relayflow.agent.domain.AiAgentInvocationStatus;
import io.odunlamizo.relayflow.agent.domain.ConversationAiDraft;
import io.odunlamizo.relayflow.agent.domain.ExtractionField;
import io.odunlamizo.relayflow.agent.domain.WorkflowMapping;
import io.odunlamizo.relayflow.agent.repository.AiAgentConfigurationRepository;
import io.odunlamizo.relayflow.agent.repository.AiAgentInvocationLogRepository;
import io.odunlamizo.relayflow.agent.repository.ConversationAiDraftRepository;
import io.odunlamizo.relayflow.channel.domain.ChannelAccount;
import io.odunlamizo.relayflow.channel.repository.ChannelAccountRepository;
import io.odunlamizo.relayflow.common.ResourceNotFoundException;
import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.messaging.domain.ConversationStatus;
import io.odunlamizo.relayflow.messaging.domain.EscalationType;
import io.odunlamizo.relayflow.messaging.repository.ConversationRepository;
import io.odunlamizo.relayflow.workflow.domain.WorkflowDefinition;
import io.odunlamizo.relayflow.workflow.domain.WorkflowRun;
import io.odunlamizo.relayflow.workflow.domain.WorkflowRunStatus;
import io.odunlamizo.relayflow.workflow.engine.WorkflowEngineService;
import io.odunlamizo.relayflow.workflow.repository.WorkflowDefinitionRepository;
import io.odunlamizo.relayflow.workflow.repository.WorkflowRunRepository;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ManualWorkflowTriggerServiceTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    private static final UUID WORKFLOW_ID = UUID.randomUUID();

    private static final UUID UNPUBLISHED_WORKFLOW_ID = UUID.randomUUID();

    @Mock private ConversationRepository conversationRepository;

    @Mock private ChannelAccountRepository channelAccountRepository;

    @Mock private AiAgentConfigurationRepository configurationRepository;

    @Mock private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Mock private WorkflowRunRepository workflowRunRepository;

    @Mock private AiAgentInvocationLogRepository invocationLogRepository;

    @Mock private ConversationAiDraftRepository draftRepository;

    @Mock private WorkflowEngineService workflowEngineService;

    @Mock private ApplicationEventPublisher eventPublisher;

    private Conversation conversation;

    private WorkflowDefinition workflowDefinition;

    @BeforeEach
    void setUp() {
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);

        ChannelAccount channelAccount = new ChannelAccount();
        channelAccount.setId(UUID.randomUUID());

        conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setWorkspace(workspace);
        conversation.setChannelAccount(channelAccount);
        conversation.setStatus(ConversationStatus.OPEN);

        AiAgentConfiguration configuration = new AiAgentConfiguration();
        configuration.setWorkflowMappings(
                List.of(
                        new WorkflowMapping(WORKFLOW_ID, "Loan request", "Wants a loan"),
                        new WorkflowMapping(UNPUBLISHED_WORKFLOW_ID, "Draft", "Not live yet")));
        configuration.setExtractionFields(
                List.of(new ExtractionField("amount", "Loan amount", null, true)));

        workflowDefinition = new WorkflowDefinition();

        when(conversationRepository.findInWorkspace(conversation.getId(), WORKSPACE_ID))
                .thenReturn(Optional.of(conversation));
        lenient()
                .when(channelAccountRepository.findAiAgentConfiguration(channelAccount.getId()))
                .thenReturn(Optional.of(configuration));
        lenient()
                .when(workflowDefinitionRepository.findEnabledIdsByWorkspace(WORKSPACE_ID))
                .thenReturn(Set.of(WORKFLOW_ID));
        lenient()
                .when(workflowDefinitionRepository.findInWorkspace(WORKFLOW_ID, WORKSPACE_ID))
                .thenReturn(Optional.of(workflowDefinition));
        lenient()
                .when(workflowRunRepository.findForConversationWithStatus(any(), any()))
                .thenReturn(List.of());
        lenient()
                .when(invocationLogRepository.findForConversationWithStatus(any(), any()))
                .thenReturn(Optional.empty());
        lenient()
                .when(draftRepository.findByConversation(conversation.getId()))
                .thenReturn(Optional.empty());
    }

    @Test
    void listsOnlyThePublishedWorkflowsOfTheConversationsAgent() {
        assertThat(service().listWorkflows(WORKSPACE_ID, conversation.getId()))
                .extracting(WorkflowMapping::workflowId)
                .containsExactly(WORKFLOW_ID);
    }

    @Test
    void runStartsTheWorkflowWithTheDraftsDataAndClearsTheEscalation() {
        conversation.setEscalatedAt(Instant.now());
        conversation.setEscalationReason("Low confidence");
        conversation.setEscalationType(EscalationType.AI_REQUESTED);
        conversation.setLockedByAiAgent(true);
        ConversationAiDraft draft = new ConversationAiDraft();
        draft.setExtractedData(Map.of("amount", "5000", "unknown", "x"));
        when(draftRepository.findByConversation(conversation.getId()))
                .thenReturn(Optional.of(draft));

        service().runWorkflow(WORKSPACE_ID, conversation.getId(), WORKFLOW_ID);

        verify(workflowEngineService)
                .executeWorkflow(
                        workflowDefinition,
                        conversation,
                        null,
                        Map.of("agent.reply", "", "agent.data.amount", "5000"));
        verify(draftRepository).delete(draft);
        assertThat(conversation.getEscalatedAt()).isNull();
        assertThat(conversation.getEscalationReason()).isNull();
        assertThat(conversation.getEscalationType()).isNull();
        assertThat(conversation.isLockedByAiAgent()).isFalse();
    }

    @Test
    void runEndsTheAgentsClarifyingTurn() {
        AiAgentInvocationLog clarifying = new AiAgentInvocationLog();
        clarifying.setStatus(AiAgentInvocationStatus.CLARIFYING);
        when(invocationLogRepository.findForConversationWithStatus(
                        conversation.getId(), AiAgentInvocationStatus.CLARIFYING))
                .thenReturn(Optional.of(clarifying));

        service().runWorkflow(WORKSPACE_ID, conversation.getId(), WORKFLOW_ID);

        assertThat(clarifying.getStatus()).isEqualTo(AiAgentInvocationStatus.SENT);
        assertThat(clarifying.getFinishedAt()).isNotNull();
    }

    @Test
    void runRefusesAWorkflowTheAgentDoesNotHavePublished() {
        assertThatThrownBy(
                        () ->
                                service()
                                        .runWorkflow(
                                                WORKSPACE_ID,
                                                conversation.getId(),
                                                UNPUBLISHED_WORKFLOW_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(workflowEngineService, never()).executeWorkflow(any(), any(), any(), any());
    }

    @Test
    void runRefusesWhileAWorkflowIsWaiting() {
        when(workflowRunRepository.findForConversationWithStatus(
                        conversation.getId(), WorkflowRunStatus.WAITING))
                .thenReturn(List.of(new WorkflowRun()));

        assertThatThrownBy(
                        () ->
                                service()
                                        .runWorkflow(
                                                WORKSPACE_ID, conversation.getId(), WORKFLOW_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already running");
    }

    @Test
    void runRefusesWhileTheAgentIsReplying() {
        when(invocationLogRepository.findForConversationWithStatus(
                        conversation.getId(), AiAgentInvocationStatus.RUNNING))
                .thenReturn(Optional.of(new AiAgentInvocationLog()));

        assertThatThrownBy(
                        () ->
                                service()
                                        .runWorkflow(
                                                WORKSPACE_ID, conversation.getId(), WORKFLOW_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI agent is replying");
    }

    @Test
    void runRefusesAClosedConversation() {
        conversation.setStatus(ConversationStatus.CLOSED);

        assertThatThrownBy(
                        () ->
                                service()
                                        .runWorkflow(
                                                WORKSPACE_ID, conversation.getId(), WORKFLOW_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("closed");
    }

    private ManualWorkflowTriggerService service() {
        return new ManualWorkflowTriggerService(
                conversationRepository,
                channelAccountRepository,
                configurationRepository,
                new PublishedWorkflowMappings(workflowDefinitionRepository),
                workflowDefinitionRepository,
                workflowRunRepository,
                invocationLogRepository,
                draftRepository,
                workflowEngineService,
                eventPublisher);
    }
}
