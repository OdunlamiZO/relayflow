package io.odunlamizo.relayflow.workflow.engine;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            String path = parts[0].trim();

            // secrets.* stays untouched so a decrypted value never reaches variables or snapshots.
            if (path.startsWith("secrets.")) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            Object value = resolve(path, variables);
            String replacement = value != null ? value.toString() : "";

            for (int index = 1; index < parts.length; index++) {
                replacement = applyFilter(replacement, parts[index].trim());
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private static String applyFilter(String value, String filter) {
        return VariableFilter.fromToken(filter)
                .map(variableFilter -> variableFilter.transform.apply(value))
                .orElse(value);
    }

    private enum VariableFilter {
        UPPER("upper", text -> text.toUpperCase(Locale.ROOT)),
        LOWER("lower", text -> text.toLowerCase(Locale.ROOT)),
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

        private static String toTitleCase(String value) {
            StringBuilder result = new StringBuilder(value.length());
            boolean capitalizeNext = true;

            for (char character : value.toCharArray()) {
                if (Character.isWhitespace(character)) {
                    capitalizeNext = true;
                    result.append(character);
                } else if (capitalizeNext) {
                    result.append(Character.toUpperCase(character));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(character));
                }
            }

            return result.toString();
        }
    }

    @SuppressWarnings("unchecked")
    static Object resolve(String path, Map<String, Object> variables) {
        if (variables.containsKey(path)) {
            return variables.get(path);
        }

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
