package com.relayflow.api.workflow.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.relayflow.api.hook.HookOutcome;
import com.relayflow.api.hook.HookService;
import com.relayflow.api.workflow.engine.ReplyRouter.ReplyRouting;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReplyRouterTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    @Mock private HookService hookService;

    private final ExecutionContext context =
            new ExecutionContext(UUID.randomUUID(), UUID.randomUUID(), WORKSPACE_ID, Map.of());

    private ReplyRouter router() {
        return new ReplyRouter(hookService);
    }

    @Test
    void genericReplyWithoutAHookIsStoredAsIs() {
        GraphNode node = genericNode(Map.of("responseVariable", " email "));

        ReplyRouting routing = router().route(node, context, " Ada@Example.com", 0);

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

        ReplyRouting routing = router().route(node, context, "Ada@X.com", 0);

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

        ReplyRouting routing = router().route(node, context, "nope", 1);

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

        ReplyRouting routing = router().route(node, context, "nope", 2);

        assertThat(routing.isRetry()).isFalse();
        assertThat(routing.nextHandle()).isEqualTo(ReplyRouter.INVALID_HANDLE);
    }

    @Test
    void maxAttemptsOfOneNeverRetries() {
        GraphNode node = genericNode(Map.of("validationHook", "builtin:email", "maxAttempts", 1));
        when(hookService.runHook(any(), any(), any(), anyMap(), any()))
                .thenReturn(HookOutcome.rejected("Not an email", List.of()));

        ReplyRouting routing = router().route(node, context, "nope", 0);

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

        ReplyRouting routing = router().route(node, context, "nope", 0);

        assertThat(routing.retryMessage()).isEqualTo("Email please");
    }

    @Test
    void hookErrorFailsTheNode() {
        GraphNode node = genericNode(Map.of("validationHook", "custom:missing"));
        when(hookService.runHook(any(), any(), any(), anyMap(), any()))
                .thenReturn(HookOutcome.error("Hook not found: custom:missing", List.of()));

        assertThatThrownBy(() -> router().route(node, context, "anything", 0))
                .isInstanceOf(NodeExecutionException.class)
                .hasMessageContaining("Hook not found");
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

        assertThat(router().route(node, context, " pro ", 0).nextHandle()).isEqualTo("pro");
        assertThat(context.getVariables()).containsEntry("plan", "Pro");

        assertThat(router().route(node, context, "1", 0).nextHandle()).isEqualTo("basic");
        assertThat(router().route(node, context, "enterprise", 0).nextHandle())
                .isEqualTo("default");
        assertThat(context.getVariables()).containsEntry("plan", "enterprise");
    }

    private GraphNode genericNode(Map<String, Object> data) {
        return new GraphNode("question", "waitForReply", data);
    }
}
