package com.relayflow.api.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.relayflow.api.agent.domain.AiAgentConfiguration;
import com.relayflow.api.agent.domain.AiAgentInvocationLog;
import com.relayflow.api.agent.domain.ConversationAiDraft;
import com.relayflow.api.agent.domain.ExtractionField;
import com.relayflow.api.agent.llm.AgentLlmRequest;
import com.relayflow.api.agent.llm.AgentLlmResponse;
import com.relayflow.api.agent.llm.LlmClient;
import com.relayflow.api.agent.llm.LlmClientFactory;
import com.relayflow.api.agent.llm.LlmMessage;
import com.relayflow.api.agent.repository.AiAgentInvocationLogRepository;
import com.relayflow.api.agent.repository.ConversationAiDraftRepository;
import com.relayflow.api.channel.domain.ChannelAccount;
import com.relayflow.api.hook.HookOutcome;
import com.relayflow.api.hook.HookService;
import com.relayflow.api.messaging.MessagingService;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.EscalationType;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.sse.SseBroadcastEvent;
import com.relayflow.api.sse.SseEventType;
import com.relayflow.api.workflow.engine.WorkflowEngineService;
import com.relayflow.api.workflow.repository.WorkflowDefinitionRepository;
import com.relayflow.api.workflow.repository.WorkflowRunRepository;
import com.relayflow.api.workspace.domain.Workspace;
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
        when(aiAgentConfigurationService.resolveEnabledConfiguration(
                        workspace.getId(), channelAccount.getId()))
                .thenReturn(Optional.of(configuration));
        when(slotClaimer.tryClaim(conversation, triggeringMessage))
                .thenReturn(Optional.of(invocationLog));
    }

    private AiAgentInvocationService service() {
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
                new ExtractedDataValidator(hookService));
    }

    private void stubLlmResponse(boolean failed) {
        when(llmClientFactory.getClient(configuration.getLlmProvider())).thenReturn(llmClient);
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
        when(contextAssembler.assemble(configuration, conversation))
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
    void successfulLlmCallKeepsEscalationsThatNeedAHuman(EscalationType escalationType) {
        escalate(escalationType);
        stubLlmResponse(false);

        service().invoke(conversation, triggeringMessage);

        assertThat(conversation.getEscalatedAt()).isNotNull();
        assertThat(conversation.getEscalationType()).isEqualTo(escalationType);
        assertThat(conversation.getEscalationReason()).isEqualTo("Earlier escalation");
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
        when(contextAssembler.assemble(configuration, conversation))
                .thenReturn(
                        new AgentLlmRequest(
                                "system",
                                List.of(LlmMessage.user("My email is john@gmail")),
                                null,
                                configuration.getExtractionFields()));
        when(llmClientFactory.getClient(configuration.getLlmProvider())).thenReturn(llmClient);
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
        verify(llmClient, times(2)).complete(requests.capture());

        AgentLlmRequest correctionRequest = requests.getAllValues().get(1);
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
                        reply("", Map.of(), true));

        service().invoke(conversation, triggeringMessage);

        assertThat(savedDraft().getProposedReply()).isEqualTo("That email looks incomplete.");
    }
}
