package com.relayflow.api.workflow.engine;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code {{variableName}}} placeholders in strings using an execution context variable
 * map.
 *
 * <p>Both flat keys ({@code {{myVar}}}) and dotted paths ({@code {{contact.name}}}) are supported.
 * The variable map stores keys in dot-notation form (e.g. {@code "contact.name"}), so dotted
 * references resolve directly as a flat lookup before falling back to nested-map traversal.
 */
public final class VariableInterpolator {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([^}]+)}}");

    private VariableInterpolator() {}

    public static String interpolate(String template, Map<String, Object> variables) {
        if (template == null || template.isBlank()) {
            return template;
        }

        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String path = matcher.group(1).trim();
            Object value = resolve(path, variables);
            String replacement = value != null ? value.toString() : "";

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Resolves a dotted path against the variable map.
     *
     * <p>First tries a direct flat lookup (e.g. {@code "contact.name"} as a single key), then walks
     * the map hierarchy segment by segment.
     */
    @SuppressWarnings("unchecked")
    static Object resolve(String path, Map<String, Object> variables) {
        if (variables.containsKey(path)) {
            return variables.get(path);
        }

        // Walk hierarchy: contact.name → variables["contact"]["name"]
        String[] parts = path.split("\\.", 2);

        if (parts.length == 2) {
            Object parent = variables.get(parts[0]);

            if (parent instanceof Map<?, ?> nested) {
                return resolve(parts[1], (Map<String, Object>) nested);
            }
        }

        return null;
    }
}
