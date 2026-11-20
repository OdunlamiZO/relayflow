package io.odunlamizo.relayflow.agent;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.odunlamizo.relayflow.agent.domain.AiAgentConfiguration;
import io.odunlamizo.relayflow.agent.domain.AiAgentInvocationLog;
import io.odunlamizo.relayflow.agent.domain.AutonomyCeiling;
import io.odunlamizo.relayflow.agent.domain.ConversationAiDraft;
import io.odunlamizo.relayflow.agent.domain.ExtractionField;
import io.odunlamizo.relayflow.agent.llm.AgentLlmRequest;
import io.odunlamizo.relayflow.agent.llm.AgentLlmResponse;
import io.odunlamizo.relayflow.agent.llm.LlmClient;
import io.odunlamizo.relayflow.agent.llm.LlmClientFactory;
import io.odunlamizo.relayflow.agent.llm.LlmMessage;
import io.odunlamizo.relayflow.agent.repository.AiAgentInvocationLogRepository;
import io.odunlamizo.relayflow.agent.repository.ConversationAiDraftRepository;
import io.odunlamizo.relayflow.channel.domain.ChannelAccount;
import io.odunlamizo.relayflow.hook.HookService;
import io.odunlamizo.relayflow.hook.domain.HookOutcome;
import io.odunlamizo.relayflow.knowledge.KnowledgeTemplateRenderer;
import io.odunlamizo.relayflow.knowledge.RenderedKnowledge;
import io.odunlamizo.relayflow.messaging.MessagingService;
import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.messaging.domain.EscalationType;
import io.odunlamizo.relayflow.messaging.domain.Message;
import io.odunlamizo.relayflow.messaging.dto.CreateMessageRequest;
import io.odunlamizo.relayflow.messaging.repository.ConversationRepository;
import io.odunlamizo.relayflow.sse.SseBroadcastEvent;
import io.odunlamizo.relayflow.sse.SseEventType;
import io.odunlamizo.relayflow.workflow.engine.WorkflowEngineService;
import io.odunlamizo.relayflow.workflow.repository.WorkflowDefinitionRepository;
import io.odunlamizo.relayflow.workflow.repository.WorkflowRunRepository;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceProviderSettings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AiAgentInvocationServiceTest {

    @Mock private AiAgentConfigurationService aiAgentConfigurationService;

    @Mock private AiAgentInvocationLogRepository invocationLogRepository;

    @Mock private ConversationAiDraftRepository draftRepository;

    @Mock private WorkflowRunRepository workflowRunRepository;

    @Mock private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Mock private WorkflowEngineService workflowEngineService;

    @Mock private AiAgentContextAssembler contextAssembler;

    @Mock private LlmClientFactory llmClientFactory;

    @Mock private LlmClient llmClient;

    @Mock private MessagingService messagingService;

    @Mock private ConversationRepository conversationRepository;

    @Mock private ApplicationEventPublisher eventPublisher;

    @Mock private AiAgentInvocationSlotClaimer slotClaimer;

    @Mock private ContactCustomFieldWriter contactCustomFieldWriter;

    @Mock private HookService hookService;

    @Mock private KnowledgeTemplateRenderer knowledgeRenderer;

    private final AiAgentConfiguration configuration = new AiAgentConfiguration();

    private Conversation conversation;

    private Message triggeringMessage;

    @BeforeEach
    void setUp() {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        ChannelAccount channelAccount = new ChannelAccount();
        channelAccount.setId(UUID.randomUUID());

        conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setWorkspace(workspace);
        conversation.setChannelAccount(channelAccount);
        conversation.setSessionStartedAt(Instant.now());

        triggeringMessage = new Message();
        triggeringMessage.setText("Hello");

        AiAgentInvocationLog invocationLog = new AiAgentInvocationLog();
        invocationLog.setConversation(conversation);

        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));
        lenient()
                .when(
                        aiAgentConfigurationService.resolveEnabledConfiguration(
                                workspace.getId(), channelAccount.getId()))
                .thenReturn(Optional.of(configuration));
        lenient()
                .when(slotClaimer.tryClaim(conversation, triggeringMessage))
                .thenReturn(Optional.of(invocationLog));
    }

    private AiAgentInvocationService service() {
        lenient()
                .when(knowledgeRenderer.renderKnowledgeBase(any(), any()))
                .thenAnswer(invocation -> RenderedKnowledge.unchanged(invocation.getArgument(1)));

        return new AiAgentInvocationService(
                aiAgentConfigurationService,
                invocationLogRepository,
                draftRepository,
                workflowRunRepository,
                workflowDefinitionRepository,
                workflowEngineService,
                contextAssembler,
                llmClientFactory,
                messagingService,
                conversationRepository,
                eventPublisher,
                slotClaimer,
                contactCustomFieldWriter,
                new ExtractedDataValidator(hookService),
                new PublishedWorkflowMappings(workflowDefinitionRepository),
                knowledgeRenderer);
    }

    private void stubLlmResponse(boolean failed) {
        when(llmClientFactory.getClient(
                        WorkspaceProviderSettings.NONE, configuration.getLlmProvider()))
                .thenReturn(llmClient);
        when(llmClient.complete(any()))
                .thenReturn(
                        new AgentLlmResponse(
                                "Hi there", "high", List.of(), false, false, Map.of(), failed));
    }

    private void escalate(EscalationType escalationType) {
        conversation.setEscalatedAt(Instant.now());
        conversation.setEscalationReason("Earlier escalation");
        conversation.setEscalationType(escalationType);
    }

    @Test
    void failedLlmCallEscalatesAsLlmFailed() {
        stubLlmResponse(true);

        service().invoke(conversation, triggeringMessage);

        assertThat(conversation.getEscalatedAt()).isNotNull();
        assertThat(conversation.getEscalationType()).isEqualTo(EscalationType.LLM_FAILED);
        assertThat(conversation.getEscalationReason()).isEqualTo("LLM call failed");
    }

    @Test
    void unexpectedPipelineErrorEscalatesAsInternalError() {
        when(contextAssembler.assemble(eq(configuration), eq(conversation), any()))
                .thenThrow(new IllegalStateException("boom"));

        service().invoke(conversation, triggeringMessage);

        assertThat(conversation.getEscalatedAt()).isNotNull();
        assertThat(conversation.getEscalationType()).isEqualTo(EscalationType.INTERNAL_ERROR);
        assertThat(conversation.getEscalationReason()).isEqualTo("Internal error");
    }

    @ParameterizedTest
    @EnumSource(
            value = EscalationType.class,
            names = {"LLM_FAILED", "INTERNAL_ERROR"})
    void successfulLlmCallClearsTemporaryEscalations(EscalationType escalationType) {
        escalate(escalationType);
        stubLlmResponse(false);

        service().invoke(conversation, triggeringMessage);

        assertThat(conversation.getEscalatedAt()).isNull();
        assertThat(conversation.getEscalationReason()).isNull();
        assertThat(conversation.getEscalationType()).isNull();

        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(events.capture());

        assertThat(events.getAllValues())
                .anySatisfy(
                        event ->
                                assertThat(((SseBroadcastEvent) event).eventType())
                                        .isEqualTo(SseEventType.CONVERSATION_UPDATED));
    }

    @ParameterizedTest
    @EnumSource(
            value = EscalationType.class,
            names = {"KEYWORD_MATCHED", "AI_REQUESTED"})
    void staysQuietWhileAnEscalationWaitsForAHuman(EscalationType escalationType) {
        escalate(escalationType);

        service().invoke(conversation, triggeringMessage);

        verify(llmClient, never()).complete(any());
        verify(messagingService, never()).createMessage(any(), any(), any(), any());
        assertThat(conversation.getEscalationType()).isEqualTo(escalationType);
        assertThat(conversation.getEscalationReason()).isEqualTo("Earlier escalation");
    }

    @Test
    void repliesAgainWhenTheEscalationIsFromAnEarlierSession() {
        escalate(EscalationType.AI_REQUESTED);
        conversation.setEscalatedAt(conversation.getSessionStartedAt().minusSeconds(3600));
        stubLlmResponse(false);

        service().invoke(conversation, triggeringMessage);

        verify(llmClient).complete(any());
    }

    @Test
    void aiRequestedEscalationSendsTheReplyThenEscalates() {
        configuration.setAutonomyCeiling(AutonomyCeiling.AUTO_SEND);
        stubLlm(
                new AgentLlmResponse(
                        "Sure, I'll connect you with a team member.",
                        "high",
                        List.of(),
                        true,
                        false,
                        Map.of(),
                        false));

        service().invoke(conversation, triggeringMessage);

        assertThat(sentText()).isEqualTo("Sure, I'll connect you with a team member.");
        assertThat(conversation.getEscalationType()).isEqualTo(EscalationType.AI_REQUESTED);
    }

    @Test
    void aiRequestedEscalationWithoutAReplySendsTheHandoffMessage() {
        configuration.setAutonomyCeiling(AutonomyCeiling.AUTO_SEND);
        configuration.setHandoffMessage("A team member will reply here.");
        stubLlm(new AgentLlmResponse("", null, List.of(), true, false, Map.of(), false));

        service().invoke(conversation, triggeringMessage);

        assertThat(sentText()).isEqualTo("A team member will reply here.");
    }

    @Test
    void keywordEscalationSendsTheHandoffMessage() {
        configuration.setAutonomyCeiling(AutonomyCeiling.AUTO_SEND);
        configuration.setEscalationKeywords(List.of("team member"));
        triggeringMessage.setText("Please connect me with a team member");

        service().invoke(conversation, triggeringMessage);

        verify(llmClient, never()).complete(any());
        assertThat(sentText()).isEqualTo(AiAgentConfiguration.DEFAULT_HANDOFF_MESSAGE);
        assertThat(conversation.getEscalationType()).isEqualTo(EscalationType.KEYWORD_MATCHED);
    }

    @Test
    void draftOnlyEscalationSendsNothing() {
        configuration.setEscalationKeywords(List.of("team member"));
        triggeringMessage.setText("Please connect me with a team member");

        service().invoke(conversation, triggeringMessage);

        verify(messagingService, never()).createMessage(any(), any(), any(), any());
        assertThat(conversation.getEscalationType()).isEqualTo(EscalationType.KEYWORD_MATCHED);
    }

    @Test
    void clarifyingQuestionIsSentInsteadOfDrafted() {
        configuration.setAutonomyCeiling(AutonomyCeiling.AUTO_SEND);
        stubLlm(
                new AgentLlmResponse(
                        "I'm not sure I understand. Could you tell me more?",
                        "low",
                        List.of(),
                        false,
                        true,
                        Map.of(),
                        false));

        service().invoke(conversation, triggeringMessage);

        assertThat(sentText()).isEqualTo("I'm not sure I understand. Could you tell me more?");
        verify(draftRepository, never()).save(any());
    }

    @Test
    void lowConfidenceAnswerIsStillDrafted() {
        configuration.setAutonomyCeiling(AutonomyCeiling.AUTO_SEND);
        stubLlm(new AgentLlmResponse("Maybe 5%?", "low", List.of(), false, false, Map.of(), false));

        service().invoke(conversation, triggeringMessage);

        verify(messagingService, never()).createMessage(any(), any(), any(), any());
        assertThat(savedDraft().getProposedReply()).isEqualTo("Maybe 5%?");
    }

    private void stubLlm(AgentLlmResponse response) {
        when(llmClientFactory.getClient(
                        WorkspaceProviderSettings.NONE, configuration.getLlmProvider()))
                .thenReturn(llmClient);
        when(llmClient.complete(any())).thenReturn(response);
    }

    private String sentText() {
        ArgumentCaptor<CreateMessageRequest> request =
                ArgumentCaptor.forClass(CreateMessageRequest.class);
        verify(messagingService).createMessage(any(), any(), request.capture(), any());

        return request.getValue().text();
    }

    private AgentLlmResponse reply(String text, Map<String, String> extractedData, boolean failed) {
        return new AgentLlmResponse(text, "high", List.of(), false, false, extractedData, failed);
    }

    private void rejectExtractedEmail() {
        configuration.setExtractionFields(
                List.of(new ExtractionField("email", "Email address", "builtin:email")));
        when(hookService.runHook(
                        eq(conversation.getWorkspace().getId()),
                        eq("builtin:email"),
                        eq("john@gmail"),
                        anyMap(),
                        isNull()))
                .thenReturn(HookOutcome.rejected("That email looks incomplete.", List.of()));
        when(contextAssembler.assemble(eq(configuration), eq(conversation), any()))
                .thenReturn(
                        new AgentLlmRequest(
                                "system",
                                List.of(LlmMessage.user("My email is john@gmail")),
                                null,
                                configuration.getExtractionFields()));
        when(llmClientFactory.getClient(
                        WorkspaceProviderSettings.NONE, configuration.getLlmProvider()))
                .thenReturn(llmClient);
    }

    private ConversationAiDraft savedDraft() {
        ArgumentCaptor<ConversationAiDraft> draft =
                ArgumentCaptor.forClass(ConversationAiDraft.class);
        verify(draftRepository).save(draft.capture());

        return draft.getValue();
    }

    @Test
    void rejectedExtractedValueIsNotStoredAndTheReplyAsksForACorrection() {
        rejectExtractedEmail();
        when(llmClient.complete(any()))
                .thenReturn(
                        reply("Thanks! We open at 9am.", Map.of("email", "john@gmail"), false),
                        reply("", Map.of(), false),
                        reply(
                                "We open at 9am. Could you resend your full email?",
                                Map.of(),
                                false));

        service().invoke(conversation, triggeringMessage);

        verify(contactCustomFieldWriter).apply(eq(conversation), eq(Map.of()), any());
        assertThat(savedDraft().getProposedReply())
                .isEqualTo("We open at 9am. Could you resend your full email?");
        assertThat(savedDraft().getExtractedData()).doesNotContainKey("email");

        ArgumentCaptor<AgentLlmRequest> requests = ArgumentCaptor.forClass(AgentLlmRequest.class);
        verify(llmClient, times(3)).complete(requests.capture());

        AgentLlmRequest correctionRequest = requests.getAllValues().get(2);
        assertThat(correctionRequest.systemPrompt())
                .contains("# CORRECTION REQUIRED")
                .contains("\"john@gmail\"")
                .contains("That email looks incomplete.")
                .contains("Thanks! We open at 9am.");
        assertThat(correctionRequest.extractionFields()).isEmpty();
    }

    @Test
    void rejectedExtractedValueFallsBackToTheHookMessageWhenTheRewriteFails() {
        rejectExtractedEmail();
        when(llmClient.complete(any()))
                .thenReturn(
                        reply("Thanks! We open at 9am.", Map.of("email", "john@gmail"), false),
                        reply("", Map.of(), true),
                        reply("", Map.of(), true));

        service().invoke(conversation, triggeringMessage);

        assertThat(savedDraft().getProposedReply()).isEqualTo("That email looks incomplete.");
    }

    @Test
    void rejectedValueThatConvertsCleanlyIsAcceptedWithoutAskingTheContact() {
        configuration.setExtractionFields(
                List.of(new ExtractionField("amount", "Contribution amount", "builtin:number")));
        when(hookService.runHook(
                        eq(conversation.getWorkspace().getId()),
                        eq("builtin:number"),
                        eq("fifty thousand naira"),
                        anyMap(),
                        isNull()))
                .thenReturn(HookOutcome.rejected("Please reply with a number.", List.of()));
        when(hookService.runHook(
                        eq(conversation.getWorkspace().getId()),
                        eq("builtin:number"),
                        eq("50000"),
                        anyMap(),
                        isNull()))
                .thenReturn(HookOutcome.accepted(50000L, Map.of(), List.of()));
        when(contextAssembler.assemble(eq(configuration), eq(conversation), any()))
                .thenReturn(
                        new AgentLlmRequest(
                                "system",
                                List.of(
                                        LlmMessage.user(
                                                "I want to contribute fifty thousand naira")),
                                null,
                                configuration.getExtractionFields()));
        when(llmClientFactory.getClient(
                        WorkspaceProviderSettings.NONE, configuration.getLlmProvider()))
                .thenReturn(llmClient);
        when(llmClient.complete(any()))
                .thenReturn(
                        reply(
                                "Great, let's get that started.",
                                Map.of("amount", "fifty thousand naira"),
                                false),
                        reply("50000", Map.of(), false));

        service().invoke(conversation, triggeringMessage);

        verify(contactCustomFieldWriter)
                .apply(eq(conversation), eq(Map.of("amount", "50000")), any());
        assertThat(savedDraft().getProposedReply()).isEqualTo("Great, let's get that started.");
        verify(llmClient, times(2)).complete(any());
    }
}
