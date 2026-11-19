package io.odunlamizo.relayflow.workflow.engine;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.odunlamizo.relayflow.hook.HookService;
import io.odunlamizo.relayflow.hook.domain.HookOutcome;
import io.odunlamizo.relayflow.workflow.engine.ReplyRouter.ReplyRouting;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReplyRouterTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    @Mock private HookService hookService;

    @Mock private VoiceReplyConverter voiceReplyConverter;

    private final ExecutionContext context =
            new ExecutionContext(UUID.randomUUID(), UUID.randomUUID(), WORKSPACE_ID, Map.of());

    private ReplyRouter router() {
        return new ReplyRouter(hookService, voiceReplyConverter);
    }

    @Test
    void genericReplyWithoutAHookIsStoredAsIs() {
        GraphNode node = genericNode(Map.of("responseVariable", " email "));

        ReplyRouting routing = router().route(node, context, " Ada@Example.com", 0, false);

        assertThat(routing.isRetry()).isFalse();
        assertThat(routing.nextHandle()).isNull();
        assertThat(routing.output()).isEmpty();
        assertThat(context.getVariables()).containsEntry("email", " Ada@Example.com");
        verifyNoInteractions(hookService);
    }

    @Test
    void acceptedReplyStoresTheHookValueAndExtraVariables() {
        GraphNode node =
                genericNode(Map.of("responseVariable", "email", "validationHook", "builtin:email"));
        when(hookService.runHook(
                        eq(WORKSPACE_ID), eq("builtin:email"), eq("Ada@X.com"), anyMap(), any()))
                .thenReturn(
                        HookOutcome.accepted(
                                "ada@x.com", Map.of("email.domain", "x.com"), List.of()));

        ReplyRouting routing = router().route(node, context, "Ada@X.com", 0, false);

        assertThat(routing.nextHandle()).isEqualTo(ReplyRouter.VALID_HANDLE);
        assertThat(context.getVariables())
                .containsEntry("email", "ada@x.com")
                .containsEntry("email.domain", "x.com");
        assertThat(routing.output()).containsEntry("validationOutcome", "ACCEPTED");
    }

    @Test
    void rejectedReplyWithAttemptsLeftAsksAgainWithoutStoringAnything() {
        GraphNode node =
                genericNode(
                        Map.of(
                                "responseVariable", "email",
                                "validationHook", "builtin:email",
                                "maxAttempts", 3));
        when(hookService.runHook(any(), any(), any(), anyMap(), any()))
                .thenReturn(HookOutcome.rejected("Not an email", List.of()));

        ReplyRouting routing = router().route(node, context, "nope", 1, false);

        assertThat(routing.isRetry()).isTrue();
        assertThat(routing.retryMessage()).isEqualTo("Not an email");
        assertThat(routing.output()).containsEntry("attempt", 2);
        assertThat(context.getVariables()).doesNotContainKey("email");
    }

    @Test
    void rejectedReplyOnTheLastAttemptFollowsTheInvalidHandle() {
        GraphNode node = genericNode(Map.of("validationHook", "builtin:email", "maxAttempts", 3));
        when(hookService.runHook(any(), any(), any(), anyMap(), any()))
                .thenReturn(HookOutcome.rejected("Not an email", List.of()));

        ReplyRouting routing = router().route(node, context, "nope", 2, false);

        assertThat(routing.isRetry()).isFalse();
        assertThat(routing.nextHandle()).isEqualTo(ReplyRouter.INVALID_HANDLE);
    }

    @Test
    void maxAttemptsOfOneNeverRetries() {
        GraphNode node = genericNode(Map.of("validationHook", "builtin:email", "maxAttempts", 1));
        when(hookService.runHook(any(), any(), any(), anyMap(), any()))
                .thenReturn(HookOutcome.rejected("Not an email", List.of()));

        ReplyRouting routing = router().route(node, context, "nope", 0, false);

        assertThat(routing.nextHandle()).isEqualTo(ReplyRouter.INVALID_HANDLE);
    }

    @Test
    void nodeErrorMessageOverrideIsPassedToTheHook() {
        GraphNode node =
                genericNode(
                        Map.of(
                                "validationHook", "builtin:email",
                                "validationErrorMessage", "Email please"));
        when(hookService.runHook(any(), any(), any(), anyMap(), eq("Email please")))
                .thenReturn(HookOutcome.rejected("Email please", List.of()));

        ReplyRouting routing = router().route(node, context, "nope", 0, false);

        assertThat(routing.retryMessage()).isEqualTo("Email please");
    }

    @Test
    void hookErrorFailsTheNode() {
        GraphNode node = genericNode(Map.of("validationHook", "custom:missing"));
        when(hookService.runHook(any(), any(), any(), anyMap(), any()))
                .thenReturn(HookOutcome.error("Hook not found: custom:missing", List.of()));

        assertThatThrownBy(() -> router().route(node, context, "anything", 0, false))
                .isInstanceOf(NodeExecutionException.class)
                .hasMessageContaining("Hook not found");
    }

    @Test
    void rejectedVoiceReplyIsConvertedAndCheckedAgain() {
        GraphNode node =
                genericNode(
                        Map.of(
                                "question", "How much do you want to borrow?",
                                "responseVariable", "amount",
                                "validationHook", "builtin:number"));
        when(hookService.runHook(any(), any(), eq("Five thousand naira."), anyMap(), any()))
                .thenReturn(HookOutcome.rejected("Please reply with a number.", List.of()));
        when(voiceReplyConverter.convert(
                        WORKSPACE_ID,
                        "How much do you want to borrow?",
                        "Five thousand naira.",
                        "Please reply with a number."))
                .thenReturn(Optional.of("5000"));
        when(hookService.runHook(any(), any(), eq("5000"), anyMap(), any()))
                .thenReturn(HookOutcome.accepted(5000, Map.of(), List.of()));

        ReplyRouting routing = router().route(node, context, "Five thousand naira.", 0, true);

        assertThat(routing.nextHandle()).isEqualTo(ReplyRouter.VALID_HANDLE);
        assertThat(context.getVariables()).containsEntry("amount", 5000);
        assertThat(routing.output())
                .containsEntry("reply", "Five thousand naira.")
                .containsEntry("convertedReply", "5000")
                .containsEntry("validationOutcome", "ACCEPTED");
    }

    @Test
    void voiceReplyStaysRejectedWhenTheConvertedValueIsRejected() {
        GraphNode node = genericNode(Map.of("validationHook", "builtin:url"));
        when(hookService.runHook(any(), any(), any(), anyMap(), any()))
                .thenReturn(HookOutcome.rejected("Not a web address", List.of()));
        when(voiceReplyConverter.convert(any(), any(), any(), any()))
                .thenReturn(Optional.of("my website"));

        ReplyRouting routing = router().route(node, context, "My website.", 0, true);

        assertThat(routing.isRetry()).isTrue();
        assertThat(routing.retryMessage()).isEqualTo("Not a web address");
        assertThat(routing.output()).containsEntry("validationOutcome", "REJECTED");
    }

    @Test
    void voiceReplyStaysRejectedWhenNothingIsConverted() {
        GraphNode node = genericNode(Map.of("validationHook", "builtin:number"));
        when(hookService.runHook(any(), any(), any(), anyMap(), any()))
                .thenReturn(HookOutcome.rejected("Please reply with a number.", List.of()));
        when(voiceReplyConverter.convert(any(), any(), any(), any())).thenReturn(Optional.empty());

        ReplyRouting routing = router().route(node, context, "I don't know.", 0, true);

        assertThat(routing.isRetry()).isTrue();
        assertThat(routing.output()).doesNotContainKey("convertedReply");
    }

    @Test
    void typedReplyIsNeverConverted() {
        GraphNode node = genericNode(Map.of("validationHook", "builtin:number"));
        when(hookService.runHook(any(), any(), any(), anyMap(), any()))
                .thenReturn(HookOutcome.rejected("Please reply with a number.", List.of()));

        router().route(node, context, "five thousand", 0, false);

        verify(voiceReplyConverter, never()).convert(any(), any(), any(), any());
    }

    @Test
    void definedReplyMatchesOptionsByTextThenByPosition() {
        Map<String, Object> data = new HashMap<>();
        data.put("responseType", "defined");
        data.put("responseVariable", "plan");
        data.put(
                "options",
                List.of(
                        Map.of("id", "basic", "text", "Basic"),
                        Map.of("id", "pro", "text", "Pro")));
        GraphNode node = new GraphNode("question", "waitForReply", data);

        assertThat(router().route(node, context, " pro ", 0, false).nextHandle()).isEqualTo("pro");
        assertThat(context.getVariables()).containsEntry("plan", "Pro");

        assertThat(router().route(node, context, "1", 0, false).nextHandle()).isEqualTo("basic");
        assertThat(router().route(node, context, "enterprise", 0, false).nextHandle())
                .isEqualTo("default");
        assertThat(context.getVariables()).containsEntry("plan", "enterprise");
    }

    private GraphNode genericNode(Map<String, Object> data) {
        return new GraphNode("question", "waitForReply", data);
    }
}
