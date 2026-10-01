package com.relayflow.api.hook.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum BuiltInHook {
    EMAIL(
            "Email address",
            "Accepts a valid email address and stores it trimmed and lowercased.",
            "if is email(value) then lower case(trim(value)) else false",
            "That doesn't look like a valid email address. Please try again."),
    PHONE_NUMBER(
            "Phone number",
            "Accepts 7–15 digits, optionally starting with + or 00. Stores it without spaces or"
                    + " dashes.",
            "normalize phone number(value)",
            "That doesn't look like a valid phone number. Please try again."),
    NUMBER(
            "Number",
            "Accepts any number, including decimals, and stores it as a number.",
            "number(trim(value))",
            "Please reply with a number."),
    WHOLE_NUMBER(
            "Whole number",
            "Accepts a number with no decimal part and stores it as a number.",
            "{parsed: number(trim(value)), result: if parsed != null and floor(parsed) = parsed"
                    + " then parsed else false}.result",
            "Please reply with a whole number."),
    DATE(
            "Date",
            "Accepts dates like 2026-09-23, 23 September 2026, or Sep 23, 2026. Stores it as"
                    + " YYYY-MM-DD.",
            "parse date(value)",
            "Please reply with a date, for example 23 September 2026."),
    URL(
            "Web address",
            "Accepts an http or https URL.",
            "if is url(value) then trim(value) else false",
            "That doesn't look like a valid web address. Please try again.");

    public static final String KEY_PREFIX = "builtin:";

    private final String displayName;

    private final String description;

    private final String expression;

    private final String errorMessage;

    BuiltInHook(String displayName, String description, String expression, String errorMessage) {
        this.displayName = displayName;
        this.description = description;
        this.expression = expression;
        this.errorMessage = errorMessage;
    }

    public String key() {
        return KEY_PREFIX + name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public String expression() {
        return expression;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public static Optional<BuiltInHook> fromKey(String key) {
        return Arrays.stream(values()).filter(hook -> hook.key().equals(key)).findFirst();
    }
}
