package com.relayflow.api.hook;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class BuiltInHookTest {

    private static final FeelHookEvaluator evaluator = new FeelHookEvaluator();

    @AfterAll
    static void shutdown() {
        evaluator.shutdown();
    }

    @ParameterizedTest
    @EnumSource(BuiltInHook.class)
    void everyBuiltInExpressionParses(BuiltInHook hook) {
        assertThat(evaluator.findSyntaxError(hook.expression())).isEmpty();
    }

    @Test
    void keysAreLowercaseAndResolvable() {
        assertThat(BuiltInHook.PHONE_NUMBER.key()).isEqualTo("builtin:phone_number");
        assertThat(BuiltInHook.fromKey("builtin:phone_number")).contains(BuiltInHook.PHONE_NUMBER);
        assertThat(BuiltInHook.fromKey("builtin:unknown")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "' Ada@Example.COM ', ada@example.com",
        "first.last+tag@mail.example.org, first.last+tag@mail.example.org"
    })
    void emailAcceptsAndNormalizes(String reply, String expected) {
        assertAccepted(BuiltInHook.EMAIL, reply, expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ada", "ada@", "ada@example", "ada @example.com", ""})
    void emailRejectsInvalidAddresses(String reply) {
        assertRejected(BuiltInHook.EMAIL, reply);
    }

    @ParameterizedTest
    @CsvSource({
        "'+44 20 7946 0958', +442079460958",
        "'0044 (20) 7946-0958', +442079460958",
        "'080.1234.5678', 08012345678"
    })
    void phoneNumberAcceptsAndStripsSeparators(String reply, String expected) {
        assertAccepted(BuiltInHook.PHONE_NUMBER, reply, expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "call me", "+1234567890123456", "++441234567"})
    void phoneNumberRejectsInvalidNumbers(String reply) {
        assertRejected(BuiltInHook.PHONE_NUMBER, reply);
    }

    @Test
    void numberAcceptsDecimals() {
        assertAccepted(BuiltInHook.NUMBER, " 4.5 ", 4.5);
    }

    @Test
    void numberRejectsText() {
        assertRejected(BuiltInHook.NUMBER, "four");
    }

    @Test
    void wholeNumberAcceptsIntegers() {
        // FEEL's Java value mapper returns whole numbers as Long and decimals as Double.
        assertAccepted(BuiltInHook.WHOLE_NUMBER, "42", 42L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.5", "forty", ""})
    void wholeNumberRejectsEverythingElse(String reply) {
        assertRejected(BuiltInHook.WHOLE_NUMBER, reply);
    }

    @ParameterizedTest
    @CsvSource({
        "2026-09-23, 2026-09-23",
        "23 September 2026, 2026-09-23",
        "'Sep 23, 2026', 2026-09-23",
        "23 sep 2026, 2026-09-23"
    })
    void dateAcceptsIsoAndWrittenMonthFormats(String reply, String expected) {
        assertAccepted(BuiltInHook.DATE, reply, expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"09/10/2026", "31 February 2026", "tomorrow"})
    void dateRejectsAmbiguousOrImpossibleDates(String reply) {
        assertRejected(BuiltInHook.DATE, reply);
    }

    @Test
    void urlAcceptsHttpAndHttps() {
        assertAccepted(BuiltInHook.URL, " https://example.com/a ", "https://example.com/a");
    }

    @ParameterizedTest
    @ValueSource(strings = {"example.com", "ftp://example.com", "https://"})
    void urlRejectsOtherValues(String reply) {
        assertRejected(BuiltInHook.URL, reply);
    }

    private HookOutcome run(BuiltInHook hook, String reply) {
        return evaluator.evaluate(hook.expression(), reply, Map.of(), hook.errorMessage());
    }

    private void assertAccepted(BuiltInHook hook, String reply, Object expected) {
        HookOutcome outcome = run(hook, reply);

        assertThat(outcome.status()).isEqualTo(HookOutcomeStatus.ACCEPTED);
        assertThat(outcome.value()).isEqualTo(expected);
    }

    private void assertRejected(BuiltInHook hook, String reply) {
        HookOutcome outcome = run(hook, reply);

        assertThat(outcome.status()).isEqualTo(HookOutcomeStatus.REJECTED);
        assertThat(outcome.errorMessage()).isEqualTo(hook.errorMessage());
    }
}
