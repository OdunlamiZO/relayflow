package com.relayflow.api.hook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.relayflow.api.hook.domain.Hook;
import com.relayflow.api.hook.dto.SaveHookRequest;
import com.relayflow.api.hook.repository.HookRepository;
import com.relayflow.api.workflow.WorkflowValidationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class HookServiceTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    @Mock private HookRepository hookRepository;

    // A real evaluator — these tests check the service runs the stored expression, not a stub.
    private final FeelHookEvaluator evaluator = new FeelHookEvaluator();

    @AfterEach
    void shutdown() {
        evaluator.shutdown();
    }

    private HookService service() {
        return new HookService(hookRepository, evaluator);
    }

    @Test
    void createRejectsAnUnparseableExpression() {
        SaveHookRequest request = new SaveHookRequest("Order ID", null, "value ((", "Bad order ID");

        assertThatThrownBy(() -> service().createHook(WORKSPACE_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        error ->
                                assertThat(((ResponseStatusException) error).getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Invalid FEEL expression");

        verify(hookRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsADuplicateNameWithConflict() {
        SaveHookRequest request =
                new SaveHookRequest("Order ID", null, "matches(value, \"^A\")", "Bad");
        when(hookRepository.saveAndFlush(any())).thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> service().createHook(WORKSPACE_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void deleteRefusesWhileAWorkflowUsesTheHook() {
        Hook hook = hook("matches(value, \"^A\")", "Bad");
        when(hookRepository.findInWorkspace(hook.getId(), WORKSPACE_ID))
                .thenReturn(Optional.of(hook));
        when(hookRepository.findWorkflowNamesReferencing(WORKSPACE_ID, hook.key()))
                .thenReturn(List.of("Onboarding"));

        assertThatThrownBy(() -> service().deleteHook(WORKSPACE_ID, hook.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Onboarding");

        verify(hookRepository, never()).delete(any());
    }

    @Test
    void deleteRefusesWhileAnAiAgentUsesTheHook() {
        Hook hook = hook("matches(value, \"^A\")", "Bad");
        when(hookRepository.findInWorkspace(hook.getId(), WORKSPACE_ID))
                .thenReturn(Optional.of(hook));
        when(hookRepository.findAiAgentNamesReferencing(WORKSPACE_ID, hook.key()))
                .thenReturn(List.of("Support"));

        assertThatThrownBy(() -> service().deleteHook(WORKSPACE_ID, hook.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI agent Support");

        verify(hookRepository, never()).delete(any());
    }

    @Test
    void runHookEvaluatesACustomHookWithItsOwnErrorMessage() {
        Hook hook = hook("matches(value, \"^ORD-\\\\d+$\")", "Order IDs look like ORD-123");
        when(hookRepository.findInWorkspace(hook.getId(), WORKSPACE_ID))
                .thenReturn(Optional.of(hook));

        HookOutcome accepted =
                service().runHook(WORKSPACE_ID, hook.key(), "ORD-42", Map.of(), null);
        HookOutcome rejected = service().runHook(WORKSPACE_ID, hook.key(), "42", Map.of(), " ");

        assertThat(accepted.status()).isEqualTo(HookOutcomeStatus.ACCEPTED);
        assertThat(rejected.errorMessage()).isEqualTo("Order IDs look like ORD-123");
    }

    @Test
    void runHookUsesTheNodeErrorMessageOverride() {
        HookOutcome outcome =
                service().runHook(WORKSPACE_ID, "builtin:email", "nope", Map.of(), "Email please");

        assertThat(outcome.errorMessage()).isEqualTo("Email please");
    }

    @Test
    void runHookWithAnUnknownKeyIsAnError() {
        HookOutcome outcome =
                service().runHook(WORKSPACE_ID, "custom:not-a-uuid", "value", Map.of(), null);

        assertThat(outcome.status()).isEqualTo(HookOutcomeStatus.ERROR);
    }

    @Test
    void validateHookReferencesRejectsAMissingCustomHook() {
        UUID missingId = UUID.randomUUID();
        when(hookRepository.findInWorkspace(missingId, WORKSPACE_ID)).thenReturn(Optional.empty());

        Map<String, Object> graph =
                Map.of(
                        "nodes",
                        List.of(
                                Map.of(
                                        "id", "question",
                                        "type", "waitForReply",
                                        "data",
                                                Map.of(
                                                        "label",
                                                        "Ask email",
                                                        "validationHook",
                                                        "custom:" + missingId))));

        assertThatThrownBy(() -> service().validateHookReferences(WORKSPACE_ID, graph))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Ask email");
    }

    @Test
    void validateHookReferencesAcceptsBuiltIns() {
        Map<String, Object> graph =
                Map.of(
                        "nodes",
                        List.of(
                                Map.of(
                                        "id", "question",
                                        "type", "waitForReply",
                                        "data", Map.of("validationHook", "builtin:email"))));

        service().validateHookReferences(WORKSPACE_ID, graph);
    }

    private Hook hook(String expression, String errorMessage) {
        Hook hook = new Hook();
        hook.setId(UUID.randomUUID());
        hook.setWorkspaceId(WORKSPACE_ID);
        hook.setName("Order ID");
        hook.setExpression(expression);
        hook.setErrorMessage(errorMessage);

        return hook;
    }
}
