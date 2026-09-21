package com.relayflow.api.workflow.engine.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.workflow.NodeType;
import com.relayflow.api.workflow.engine.ExecutionContext;
import com.relayflow.api.workflow.engine.GraphNode;
import com.relayflow.api.workflow.engine.NodeExecutionException;
import com.relayflow.api.workflow.engine.NodeExecutionResult;
import com.relayflow.api.workflow.engine.NodeExecutor;
import com.relayflow.api.workspace.SecretService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Executes an HTTP request and maps response fields into the execution context.
 *
 * <p>Follows the {@code success} handle on 2xx responses and the {@code error} handle on non-2xx,
 * timeout, or network failure. On error, {@code {{error.message}}} is set in the context.
 */
@Component
public class HttpRequestNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestNodeExecutor.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private static final Pattern SECRET_PLACEHOLDER =
            Pattern.compile("\\{\\{secrets\\.([A-Za-z0-9_]+)}}");

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    private final SecretService secretService;

    public HttpRequestNodeExecutor(ObjectMapper objectMapper, SecretService secretService) {
        this.objectMapper = objectMapper;
        this.secretService = secretService;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public NodeType nodeType() {
        return NodeType.HTTP_REQUEST;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeExecutionResult execute(GraphNode node, ExecutionContext context) {
        Map<String, Object> data = node.data();

        String method = (String) data.getOrDefault("method", "GET");
        String rawUrl = (String) data.get("url");
        String contentType = (String) data.getOrDefault("contentType", "application/json");
        String rawBody = (String) data.get("body");
        int timeoutSeconds =
                data.get("timeoutSeconds") instanceof Number n
                        ? n.intValue()
                        : DEFAULT_TIMEOUT_SECONDS;

        List<Map<String, Object>> rawHeaders = (List<Map<String, Object>>) data.get("headers");
        List<Map<String, Object>> responseMappings =
                (List<Map<String, Object>>) data.get("responseMappings");
        String responseStatusVariable = (String) data.get("responseStatusVariable");

        if (rawUrl == null || rawUrl.isBlank()) {
            throw new NodeExecutionException("HTTP Request node has no URL configured");
        }

        String url = resolveSecrets(context.interpolate(rawUrl), context.getWorkspaceId());
        Map<String, Object> output = new LinkedHashMap<>();

        try {
            HttpRequest.Builder requestBuilder =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(timeoutSeconds));

            // Build body publisher
            HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();

            if (rawBody != null && !rawBody.isBlank()) {
                String interpolatedBody =
                        resolveSecrets(context.interpolate(rawBody), context.getWorkspaceId());
                bodyPublisher = HttpRequest.BodyPublishers.ofString(interpolatedBody);
                requestBuilder.header("Content-Type", contentType);
            }

            requestBuilder.method(method, bodyPublisher);

            // Add user-defined headers (interpolating values)
            if (rawHeaders != null) {
                for (Map<String, Object> header : rawHeaders) {
                    String key = (String) header.get("key");
                    String value = (String) header.get("value");

                    if (key != null && !key.isBlank()) {
                        String interpolatedValue =
                                resolveSecrets(
                                        context.interpolate(value != null ? value : ""),
                                        context.getWorkspaceId());
                        requestBuilder.header(key, interpolatedValue);
                    }
                }
            }

            HttpResponse<String> response =
                    httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            output.put("statusCode", statusCode);

            if (responseStatusVariable != null && !responseStatusVariable.isBlank()) {
                context.setVariable(responseStatusVariable, statusCode);
            }

            // Map response body fields into context variables
            String responseBody = response.body();

            if (responseBody != null && !responseBody.isBlank() && responseMappings != null) {
                try {
                    JsonNode root = objectMapper.readTree(responseBody);

                    for (Map<String, Object> mapping : responseMappings) {
                        String path = (String) mapping.get("path");
                        String variable = (String) mapping.get("variable");

                        if (path != null && variable != null && !variable.isBlank()) {
                            Object resolved = resolveJsonPath(root, path);

                            if (resolved != null) {
                                context.setVariable(variable, resolved);
                                output.put(variable, resolved);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn(
                            "Could not parse response body as JSON for mapping: {}",
                            e.getMessage());
                }
            }

            // 2xx → success, anything else → error
            if (statusCode >= 200 && statusCode < 300) {
                return NodeExecutionResult.handle("success", output);
            } else {
                context.setVariable("error.message", "HTTP " + statusCode);
                output.put("error", "HTTP " + statusCode);

                return NodeExecutionResult.handle("error", output);
            }

        } catch (NodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage =
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("HTTP request failed for node {}: {}", node.id(), errorMessage);

            context.setVariable("error.message", errorMessage);
            output.put("error", errorMessage);

            return NodeExecutionResult.handle("error", output);
        }
    }

    /**
     * Resolves {@code {{secrets.NAME}}} tokens left untouched by {@link
     * com.relayflow.api.workflow.engine.VariableInterpolator}. Only this executor ever calls this —
     * the decrypted value it returns must never be assigned to a workflow variable or added to
     * {@code output}, since both end up in a persisted {@code WorkflowRunStep} snapshot.
     *
     * @throws NodeExecutionException if a referenced secret doesn't exist — fails the step loudly
     *     rather than silently sending a request with a blank credential
     */
    private String resolveSecrets(String text, UUID workspaceId) {
        if (text == null || text.isBlank() || !text.contains("{{secrets.")) {
            return text;
        }

        Matcher matcher = SECRET_PLACEHOLDER.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String name = matcher.group(1);
            String value =
                    secretService
                            .resolveDecrypted(workspaceId, name)
                            .orElseThrow(
                                    () -> new NodeExecutionException("Unknown secret: " + name));

            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Resolves a dot-notation path against a Jackson {@link JsonNode}. Supports both {@code
     * "user.email"} and {@code "items[0].name"} styles.
     */
    private Object resolveJsonPath(JsonNode root, String path) {
        JsonNode current = root;

        for (String segment : path.split("\\.")) {
            if (current == null || current.isMissingNode()) {
                return null;
            }

            // Handle array index: items[0]
            int bracketOpen = segment.indexOf('[');

            if (bracketOpen >= 0) {
                String field = segment.substring(0, bracketOpen);
                int index =
                        Integer.parseInt(segment.substring(bracketOpen + 1, segment.indexOf(']')));

                current = current.path(field).path(index);
            } else {
                current = current.path(segment);
            }
        }

        if (current == null || current.isMissingNode()) {
            return null;
        }

        if (current.isTextual()) return current.asText();
        if (current.isInt() || current.isLong()) return current.asLong();
        if (current.isDouble() || current.isFloat()) return current.asDouble();
        if (current.isBoolean()) return current.asBoolean();

        try {
            return objectMapper.convertValue(current, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return current.toString();
        }
    }
}
