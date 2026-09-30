package com.relayflow.api.hook;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.camunda.feel.context.JavaFunction;
import org.camunda.feel.context.JavaFunctionProvider;
import org.camunda.feel.syntaxtree.Val;
import org.camunda.feel.syntaxtree.ValBoolean;
import org.camunda.feel.syntaxtree.ValDate;
import org.camunda.feel.syntaxtree.ValNull$;
import org.camunda.feel.syntaxtree.ValString;

public class HookFunctionProvider extends JavaFunctionProvider {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final Pattern PHONE_NUMBER_SEPARATORS = Pattern.compile("[\\s().-]");

    private static final Pattern DIGITS = Pattern.compile("^\\d{7,15}$");

    private static final List<DateTimeFormatter> DATE_FORMATS =
            List.of(
                    DateTimeFormatter.ISO_LOCAL_DATE,
                    caseInsensitive("d MMMM uuuu"),
                    caseInsensitive("d MMM uuuu"),
                    caseInsensitive("MMMM d, uuuu"),
                    caseInsensitive("MMM d, uuuu"));

    private static final Map<String, JavaFunction> FUNCTIONS =
            Map.of(
                    "is email",
                    textFunction(text -> ValBoolean.apply(isEmail(text)), false),
                    "is url",
                    textFunction(text -> ValBoolean.apply(isUrl(text)), false),
                    "normalize phone number",
                    textFunction(HookFunctionProvider::normalizePhoneNumber, true),
                    "parse date",
                    textFunction(HookFunctionProvider::parseDate, true));

    @Override
    public Optional<JavaFunction> resolveFunction(String name) {
        return Optional.ofNullable(FUNCTIONS.get(name));
    }

    @Override
    public Collection<String> getFunctionNames() {
        return FUNCTIONS.keySet();
    }

    private static boolean isEmail(String text) {
        return EMAIL.matcher(text.trim()).matches();
    }

    private static boolean isUrl(String text) {
        try {
            URI uri = new URI(text.trim());
            String scheme = uri.getScheme();

            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static Val normalizePhoneNumber(String text) {
        String stripped = PHONE_NUMBER_SEPARATORS.matcher(text.trim()).replaceAll("");

        if (stripped.startsWith("00")) {
            stripped = "+" + stripped.substring(2);
        }

        boolean international = stripped.startsWith("+");
        String digits = international ? stripped.substring(1) : stripped;

        if (!DIGITS.matcher(digits).matches()) {
            return ValNull$.MODULE$;
        }

        return ValString.apply(international ? "+" + digits : digits);
    }

    private static Val parseDate(String text) {
        String trimmed = text.trim();

        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return ValDate.apply(LocalDate.parse(trimmed, format));
            } catch (DateTimeParseException ignored) {
            }
        }

        return ValNull$.MODULE$;
    }

    private static JavaFunction textFunction(
            Function<String, Val> function, boolean nullOnNonText) {
        return new JavaFunction(
                List.of("text"),
                arguments -> {
                    if (arguments.getFirst() instanceof ValString text) {
                        return function.apply(text.value());
                    }

                    return nullOnNonText ? ValNull$.MODULE$ : ValBoolean.apply(false);
                });
    }

    private static DateTimeFormatter caseInsensitive(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH)
                // STRICT rejects impossible dates like 31 February instead of clamping them.
                .withResolverStyle(ResolverStyle.STRICT);
    }
}
