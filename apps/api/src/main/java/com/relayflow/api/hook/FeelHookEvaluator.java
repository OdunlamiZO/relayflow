package com.relayflow.api.hook;

import com.relayflow.api.hook.domain.HookOutcome;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.camunda.feel.api.EvaluationFailure;
import org.camunda.feel.api.EvaluationResult;
import org.camunda.feel.api.FeelEngineApi;
import org.camunda.feel.api.FeelEngineBuilder;
import org.camunda.feel.api.ParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Evaluates FEEL hook expressions. {@code true} accepts the value, {@code false}/{@code null}
 * rejects it, a context with a {@code valid} entry gives full control, and any other result
 * replaces the value.
 */
@Component
public class FeelHookEvaluator {

    private static final Logger log = LoggerFactory.getLogger(FeelHookEvaluator.class);

    private static final long EVALUATION_TIMEOUT_MILLISECONDS = 2_000;

    private static final int EVALUATION_THREADS = 4;

    private static final int EVALUATION_QUEUE_CAPACITY = 100;

    private final FeelEngineApi engine =
            FeelEngineBuilder.forJava()
                    .withFunctionProvider(new HookFunctionProvider())
                    .withEnabledExternalFunctions(false)
                    .build();

    private final ThreadPoolExecutor executor = newExecutor();

    public Optional<String> findSyntaxError(String expression) {
        ParseResult result = engine.parseExpression(expression);

        return result.isSuccess() ? Optional.empty() : Optional.of(result.failure().message());
    }

    public HookOutcome evaluate(
            String expression,
            String value,
            Map<String, Object> variables,
            String defaultErrorMessage) {
        Map<String, Object> feelContext = new LinkedHashMap<>();
        feelContext.put("value", value);
        feelContext.put("variables", nest(variables));

        EvaluationResult result;

        try {
            Future<EvaluationResult> future =
                    executor.submit(() -> engine.evaluateExpression(expression, feelContext));

            // FEEL can't be interrupted; a timed-out evaluation holds its thread until done.
            result = future.get(EVALUATION_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("Hook evaluation timed out after {}ms", EVALUATION_TIMEOUT_MILLISECONDS);

            return HookOutcome.error(
                    "Hook took longer than " + EVALUATION_TIMEOUT_MILLISECONDS + "ms to evaluate",
                    List.of());
        } catch (RejectedExecutionException e) {
            return HookOutcome.error("Too many hooks are being evaluated — try again", List.of());
        } catch (ExecutionException e) {
            log.warn("Hook evaluation threw an exception", e.getCause());

            return HookOutcome.error(
                    "Hook evaluation failed: " + e.getCause().getMessage(), List.of());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return HookOutcome.error("Hook evaluation was interrupted", List.of());
        }

        List<String> warnings =
                result.getSuppressedFailures().stream()
                        .map(EvaluationFailure::failureMessage)
                        .toList();

        if (result.isFailure()) {
            return HookOutcome.error(result.failure().message(), warnings);
        }

        return interpret(result.result(), value, defaultErrorMessage, warnings);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private HookOutcome interpret(
            Object result,
            String originalValue,
            String defaultErrorMessage,
            List<String> warnings) {
        if (result == null || Boolean.FALSE.equals(result)) {
            return HookOutcome.rejected(defaultErrorMessage, warnings);
        }

        if (Boolean.TRUE.equals(result)) {
            return HookOutcome.accepted(originalValue, Map.of(), warnings);
        }

        if (result instanceof Map<?, ?> map && map.containsKey("valid")) {
            if (!Boolean.TRUE.equals(map.get("valid"))) {
                String error =
                        map.get("error") instanceof String text && !text.isBlank()
                                ? text
                                : defaultErrorMessage;

                return HookOutcome.rejected(error, warnings);
            }

            Object acceptedValue =
                    map.containsKey("value") ? toPlainValue(map.get("value")) : originalValue;

            Map<String, Object> extraVariables = new LinkedHashMap<>();

            if (map.get("variables") instanceof Map<?, ?> returnedVariables) {
                returnedVariables.forEach(
                        (name, variableValue) ->
                                extraVariables.put(
                                        String.valueOf(name), toPlainValue(variableValue)));
            }

            return HookOutcome.accepted(acceptedValue, extraVariables, warnings);
        }

        return HookOutcome.accepted(toPlainValue(result), Map.of(), warnings);
    }

    private Object toPlainValue(Object value) {
        if (value == null
                || value instanceof String
                || value instanceof Boolean
                || value instanceof Number) {
            return value;
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> plain = new LinkedHashMap<>();
            map.forEach((key, entry) -> plain.put(String.valueOf(key), toPlainValue(entry)));

            return plain;
        }

        if (value instanceof List<?> list) {
            List<Object> plain = new ArrayList<>();
            list.forEach(entry -> plain.add(toPlainValue(entry)));

            return plain;
        }

        return value.toString();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> nest(Map<String, Object> variables) {
        Map<String, Object> nested = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String[] segments = entry.getKey().split("\\.");
            Map<String, Object> current = nested;
            boolean clashed = false;

            for (int index = 0; index < segments.length - 1; index++) {
                Object child =
                        current.computeIfAbsent(segments[index], key -> new LinkedHashMap<>());

                if (!(child instanceof Map)) {
                    clashed = true;
                    break;
                }

                current = (Map<String, Object>) child;
            }

            String leaf = segments[segments.length - 1];

            if (!clashed && !(current.get(leaf) instanceof Map)) {
                current.put(leaf, entry.getValue());
            }
        }

        return nested;
    }

    private static ThreadPoolExecutor newExecutor() {
        AtomicInteger threadCount = new AtomicInteger();

        return new ThreadPoolExecutor(
                EVALUATION_THREADS,
                EVALUATION_THREADS,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(EVALUATION_QUEUE_CAPACITY),
                runnable -> {
                    Thread thread =
                            new Thread(
                                    runnable,
                                    "feel-hook-evaluator-" + threadCount.incrementAndGet());
                    thread.setDaemon(true);

                    return thread;
                });
    }
}
