package com.relayflow.api.hook;

import com.relayflow.api.hook.domain.Hook;
import com.relayflow.api.hook.dto.BuiltInHookResponse;
import com.relayflow.api.hook.dto.HookResponse;
import com.relayflow.api.hook.dto.SaveHookRequest;
import com.relayflow.api.hook.dto.TestHookRequest;
import com.relayflow.api.hook.dto.TestHookResponse;
import com.relayflow.api.hook.repository.HookRepository;
import com.relayflow.api.workflow.NodeType;
import com.relayflow.api.workflow.WorkflowValidationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HookService {

    private static final Logger log = LoggerFactory.getLogger(HookService.class);

    private final HookRepository hookRepository;

    private final FeelHookEvaluator evaluator;

    public HookService(HookRepository hookRepository, FeelHookEvaluator evaluator) {
        this.hookRepository = hookRepository;
        this.evaluator = evaluator;
    }

    public List<BuiltInHookResponse> listBuiltInHooks() {
        return Arrays.stream(BuiltInHook.values())
                .map(
                        hook ->
                                new BuiltInHookResponse(
                                        hook.key(),
                                        hook.displayName(),
                                        hook.description(),
                                        hook.expression(),
                                        hook.errorMessage()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HookResponse> listHooks(UUID workspaceId) {
        return hookRepository.findByWorkspace(workspaceId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public HookResponse createHook(UUID workspaceId, SaveHookRequest request) {
        assertParses(request.expression());

        Hook hook = new Hook();
        hook.setWorkspaceId(workspaceId);
        apply(hook, request);

        save(hook);

        log.info(
                "Hook created: id={}, name={}, workspace={}",
                hook.getId(),
                hook.getName(),
                workspaceId);

        return toResponse(hook);
    }

    @Transactional
    public HookResponse updateHook(UUID workspaceId, UUID hookId, SaveHookRequest request) {
        assertParses(request.expression());

        Hook hook = getOrThrow(workspaceId, hookId);
        apply(hook, request);

        save(hook);

        log.info("Hook updated: id={}, workspace={}", hookId, workspaceId);

        return toResponse(hook);
    }

    @Transactional
    public void deleteHook(UUID workspaceId, UUID hookId) {
        Hook hook = getOrThrow(workspaceId, hookId);
        List<String> users =
                new ArrayList<>(
                        hookRepository.findWorkflowNamesReferencing(workspaceId, hook.key()));
        hookRepository.findAiAgentNamesReferencing(workspaceId, hook.key()).stream()
                .map(name -> "AI agent " + name)
                .forEach(users::add);

        if (!users.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This hook is used by "
                            + String.join(", ", users)
                            + ". Remove it from those first.");
        }

        hookRepository.delete(hook);

        log.info("Hook deleted: id={}, workspace={}", hookId, workspaceId);
    }

    public TestHookResponse testHook(TestHookRequest request) {
        HookOutcome outcome =
                evaluator.evaluate(
                        request.expression(),
                        request.value(),
                        request.variables() != null ? request.variables() : Map.of(),
                        null);

        return new TestHookResponse(
                outcome.status(),
                outcome.value(),
                outcome.errorMessage(),
                outcome.variables(),
                outcome.warnings());
    }

    @Transactional(readOnly = true)
    public HookOutcome runHook(
            UUID workspaceId,
            String hookKey,
            String value,
            Map<String, Object> variables,
            String errorMessageOverride) {
        Optional<ResolvedHook> resolved = resolve(workspaceId, hookKey);

        if (resolved.isEmpty()) {
            return HookOutcome.error("Hook not found: " + hookKey, List.of());
        }

        String errorMessage =
                errorMessageOverride != null && !errorMessageOverride.isBlank()
                        ? errorMessageOverride
                        : resolved.get().errorMessage();

        return evaluator.evaluate(resolved.get().expression(), value, variables, errorMessage);
    }

    @Transactional(readOnly = true)
    public boolean hookExists(UUID workspaceId, String hookKey) {
        return resolve(workspaceId, hookKey).isPresent();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public void validateHookReferences(UUID workspaceId, Map<String, Object> graph) {
        List<Map<String, Object>> nodes =
                (List<Map<String, Object>>) graph.getOrDefault("nodes", List.of());

        for (Map<String, Object> node : nodes) {
            if (!NodeType.WAIT_FOR_REPLY.getValue().equals(node.get("type"))) {
                continue;
            }

            Map<String, Object> data = (Map<String, Object>) node.getOrDefault("data", Map.of());

            if (!(data.get("validationHook") instanceof String hookKey) || hookKey.isBlank()) {
                continue;
            }

            if (resolve(workspaceId, hookKey).isEmpty()) {
                Object label = data.getOrDefault("label", node.get("id"));

                throw new WorkflowValidationException(
                        "Ask Question node '"
                                + label
                                + "' uses a validation hook that no longer exists");
            }
        }
    }

    private Optional<ResolvedHook> resolve(UUID workspaceId, String hookKey) {
        if (hookKey.startsWith(BuiltInHook.KEY_PREFIX)) {
            return BuiltInHook.fromKey(hookKey)
                    .map(hook -> new ResolvedHook(hook.expression(), hook.errorMessage()));
        }

        if (hookKey.startsWith(Hook.KEY_PREFIX)) {
            UUID hookId;

            try {
                hookId = UUID.fromString(hookKey.substring(Hook.KEY_PREFIX.length()));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }

            return hookRepository
                    .findInWorkspace(hookId, workspaceId)
                    .map(hook -> new ResolvedHook(hook.getExpression(), hook.getErrorMessage()));
        }

        return Optional.empty();
    }

    private void assertParses(String expression) {
        evaluator
                .findSyntaxError(expression)
                .ifPresent(
                        error -> {
                            throw new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST, "Invalid FEEL expression: " + error);
                        });
    }

    private void apply(Hook hook, SaveHookRequest request) {
        hook.setName(request.name().trim());
        hook.setDescription(
                request.description() != null && !request.description().isBlank()
                        ? request.description().trim()
                        : null);
        hook.setExpression(request.expression());
        hook.setErrorMessage(request.errorMessage().trim());
    }

    private void save(Hook hook) {
        try {
            hookRepository.saveAndFlush(hook);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A hook named \"" + hook.getName() + "\" already exists");
        }
    }

    private Hook getOrThrow(UUID workspaceId, UUID hookId) {
        return hookRepository
                .findInWorkspace(hookId, workspaceId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hook not found"));
    }

    private HookResponse toResponse(Hook hook) {
        return new HookResponse(
                hook.getId(),
                hook.key(),
                hook.getName(),
                hook.getDescription(),
                hook.getExpression(),
                hook.getErrorMessage(),
                hook.getCreatedAt(),
                hook.getUpdatedAt());
    }

    private record ResolvedHook(String expression, String errorMessage) {}
}
