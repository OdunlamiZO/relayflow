package com.relayflow.api.workflow.engine;

import com.relayflow.api.hook.HookOutcome;
import com.relayflow.api.hook.HookService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReplyRouter {

    public static final String VALID_HANDLE = "valid";

    public static final String INVALID_HANDLE = "invalid";

    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private static final int MAX_ATTEMPTS_LIMIT = 10;

    private final HookService hookService;

    public ReplyRouter(HookService hookService) {
        this.hookService = hookService;
    }

    public ReplyRouting route(
            GraphNode waitingNode,
            ExecutionContext context,
            String replyText,
            int previousInvalidAttempts) {
        String responseType = (String) waitingNode.data().getOrDefault("responseType", "generic");

        if ("defined".equals(responseType)) {
            return ReplyRouting.next(routeDefinedReply(waitingNode, context, replyText), Map.of());
        }

        if (!"generic".equals(responseType)) {
            return ReplyRouting.next(null, Map.of());
        }

        String responseVariable = responseVariable(waitingNode);

        if (!(waitingNode.data().get("validationHook") instanceof String hookKey)
                || hookKey.isBlank()) {
            if (responseVariable != null) {
                context.setVariable(responseVariable, replyText);
            }

            return ReplyRouting.next(null, Map.of());
        }

        HookOutcome outcome =
                hookService.runHook(
                        context.getWorkspaceId(),
                        hookKey,
                        replyText,
                        context.getVariables(),
                        (String) waitingNode.data().get("validationErrorMessage"));

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("reply", replyText);
        output.put("validationHook", hookKey);
        output.put("validationOutcome", outcome.status().name());

        switch (outcome.status()) {
            case ACCEPTED -> {
                if (responseVariable != null) {
                    context.setVariable(responseVariable, outcome.value());
                }

                outcome.variables().forEach(context::setVariable);
                output.put("value", outcome.value());

                return ReplyRouting.next(VALID_HANDLE, output);
            }
            case REJECTED -> {
                int attempt = previousInvalidAttempts + 1;
                output.put("attempt", attempt);

                if (attempt < maxAttempts(waitingNode)) {
                    return ReplyRouting.retry(outcome.errorMessage(), output);
                }

                return ReplyRouting.next(INVALID_HANDLE, output);
            }
            default ->
                    throw new NodeExecutionException(
                            "Validation hook " + hookKey + " failed: " + outcome.errorMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String routeDefinedReply(
            GraphNode waitingNode, ExecutionContext context, String replyText) {
        List<Map<String, Object>> options =
                (List<Map<String, Object>>) waitingNode.data().getOrDefault("options", List.of());
        String responseVariable = responseVariable(waitingNode);
        String trimmedReply = replyText.trim();

        for (int index = 0; index < options.size(); index++) {
            Map<String, Object> option = options.get(index);
            String text = (String) option.get("text");

            boolean matchedByText = text != null && text.trim().equalsIgnoreCase(trimmedReply);
            boolean matchedByNumber = trimmedReply.equals(String.valueOf(index + 1));

            if (matchedByText || matchedByNumber) {
                if (responseVariable != null) {
                    // Save the canonical option text (not the raw reply).
                    context.setVariable(
                            responseVariable, text != null ? text.trim() : trimmedReply);
                }

                return (String) option.get("id");
            }
        }

        // No option matched — save the raw reply and follow the "Other" edge.
        if (responseVariable != null) {
            context.setVariable(responseVariable, trimmedReply);
        }

        return "default";
    }

    private String responseVariable(GraphNode waitingNode) {
        String responseVariable = (String) waitingNode.data().get("responseVariable");

        return responseVariable != null && !responseVariable.isBlank()
                ? responseVariable.trim()
                : null;
    }

    private int maxAttempts(GraphNode waitingNode) {
        int maxAttempts =
                waitingNode.data().get("maxAttempts") instanceof Number number
                        ? number.intValue()
                        : DEFAULT_MAX_ATTEMPTS;

        return Math.clamp(maxAttempts, 1, MAX_ATTEMPTS_LIMIT);
    }

    public record ReplyRouting(String nextHandle, String retryMessage, Map<String, Object> output) {

        static ReplyRouting next(String nextHandle, Map<String, Object> output) {
            return new ReplyRouting(nextHandle, null, output);
        }

        static ReplyRouting retry(String retryMessage, Map<String, Object> output) {
            return new ReplyRouting(null, retryMessage, output);
        }

        public boolean isRetry() {
            return retryMessage != null;
        }
    }
}
