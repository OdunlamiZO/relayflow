package com.relayflow.api.workflow.engine;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code {{variableName}}} placeholders in strings using an execution context variable
 * map.
 *
 * <p>Both flat keys ({@code {{myVar}}}) and dotted paths ({@code {{contact.name}}}) are supported.
 * The variable map stores keys in dot-notation form (e.g. {@code "contact.name"}), so dotted
 * references resolve directly as a flat lookup before falling back to nested-map traversal.
 *
 * <p>A resolved value can be piped through one or more filters, e.g. {@code {{contact.name |
 * upper}}}, {@code {{contact.name | lower}}}, or {@code {{contact.name | title}}}. Unknown filters
 * are ignored.
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
            String[] parts = matcher.group(1).split("\\|");
            Object value = resolve(parts[0].trim(), variables);
            String replacement = value != null ? value.toString() : "";

            for (int i = 1; i < parts.length; i++) {
                replacement = applyFilter(replacement, parts[i].trim());
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Applies a named transform to an interpolated value. Unknown filters pass the value through
     * unchanged.
     */
    private static String applyFilter(String value, String filter) {
        return VariableFilter.fromToken(filter).map(f -> f.transform.apply(value)).orElse(value);
    }

    /** Transforms supported by the {@code {{variable | filter}}} pipe syntax. */
    private enum VariableFilter {
        UPPER("upper", v -> v.toUpperCase(Locale.ROOT)),
        LOWER("lower", v -> v.toLowerCase(Locale.ROOT)),
        TITLE("title", VariableFilter::toTitleCase);

        private final String token;

        private final UnaryOperator<String> transform;

        VariableFilter(String token, UnaryOperator<String> transform) {
            this.token = token;
            this.transform = transform;
        }

        static Optional<VariableFilter> fromToken(String token) {
            for (VariableFilter filter : values()) {
                if (filter.token.equalsIgnoreCase(token)) {
                    return Optional.of(filter);
                }
            }

            return Optional.empty();
        }

        /** Capitalizes the first letter of each word and lowercases the rest. */
        private static String toTitleCase(String value) {
            StringBuilder result = new StringBuilder(value.length());
            boolean capitalizeNext = true;

            for (char c : value.toCharArray()) {
                if (Character.isWhitespace(c)) {
                    capitalizeNext = true;
                    result.append(c);
                } else if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }

            return result.toString();
        }
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
