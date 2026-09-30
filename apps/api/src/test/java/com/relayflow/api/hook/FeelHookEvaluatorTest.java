package com.relayflow.api.hook;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FeelHookEvaluatorTest {

    private static final String DEFAULT_ERROR = "Please try again.";

    private final FeelHookEvaluator evaluator = new FeelHookEvaluator();

    @AfterEach
    void shutdown() {
        evaluator.shutdown();
    }

    @Test
    void trueAcceptsTheOriginalValue() {
        HookOutcome outcome = evaluate("string length(value) > 2", "Ada");

        assertThat(outcome.status()).isEqualTo(HookOutcomeStatus.ACCEPTED);
        assertThat(outcome.value()).isEqualTo("Ada");
    }

    @Test
    void falseRejectsWithTheDefaultErrorMessage() {
        HookOutcome outcome = evaluate("string length(value) > 5", "Ada");

        assertThat(outcome.status()).isEqualTo(HookOutcomeStatus.REJECTED);
        assertThat(outcome.errorMessage()).isEqualTo(DEFAULT_ERROR);
    }

    @Test
    void nullRejects() {
        HookOutcome outcome = evaluate("number(value)", "not a number");

        assertThat(outcome.status()).isEqualTo(HookOutcomeStatus.REJECTED);
    }

    @Test
    void anyOtherResultAcceptsAndReplacesTheValue() {
        HookOutcome outcome = evaluate("upper case(value)", "ada");

        assertThat(outcome.status()).isEqualTo(HookOutcomeStatus.ACCEPTED);
        assertThat(outcome.value()).isEqualTo("ADA");
    }

    @Test
    void validContextControlsValueAndExtraVariables() {
        HookOutcome outcome =
                evaluate(
                        "{variables: {domain: substring after(value, \"@\")}, valid: true, value:"
                                + " lower case(value)}",
                        "Ada@Example.com");

        assertThat(outcome.status()).isEqualTo(HookOutcomeStatus.ACCEPTED);
        assertThat(outcome.value()).isEqualTo("ada@example.com");
        assertThat(outcome.variables()).containsEntry("domain", "Example.com");
    }

    @Test
    void invalidContextUsesItsOwnErrorMessage() {
        HookOutcome outcome = evaluate("{valid: false, error: \"Too short\"}", "Ada");

        assertThat(outcome.status()).isEqualTo(HookOutcomeStatus.REJECTED);
        assertThat(outcome.errorMessage()).isEqualTo("Too short");
    }

    @Test
    void invalidContextWithoutAnErrorFallsBackToTheDefault() {
        HookOutcome outcome = evaluate("{valid: false}", "Ada");

        assertThat(outcome.errorMessage()).isEqualTo(DEFAULT_ERROR);
    }

    @Test
    void datesAreStoredAsIsoStrings() {
        HookOutcome outcome = evaluate("date(value)", "2026-09-23");

        assertThat(outcome.value()).isEqualTo("2026-09-23");
    }

    @Test
    void runVariablesAreNestedByTheirDottedNames() {
        HookOutcome outcome =
                evaluator.evaluate(
                        "value = variables.contact.name",
                        "Ada",
                        Map.of("contact.name", "Ada"),
                        DEFAULT_ERROR);

        assertThat(outcome.status()).isEqualTo(HookOutcomeStatus.ACCEPTED);
    }

    @Test
    void syntaxErrorIsAnErrorOutcome() {
        HookOutcome outcome = evaluate("value ((", "Ada");

        assertThat(outcome.status()).isEqualTo(HookOutcomeStatus.ERROR);
        assertThat(outcome.errorMessage()).contains("failed to parse expression");
    }

    @Test
    void externalJavaFunctionsAreDisabled() {
        HookOutcome outcome =
                evaluate(
                        "{exit: function(code) external { java: { class: \"java.lang.System\","
                                + " method signature: \"exit(int)\" } }, result: exit(1)}.result",
                        "Ada");

        assertThat(outcome.status()).isEqualTo(HookOutcomeStatus.ERROR);
        assertThat(outcome.errorMessage()).contains("External functions are disabled");
    }

    @Test
    void findSyntaxErrorReportsOnlyUnparseableExpressions() {
        assertThat(evaluator.findSyntaxError("is email(value)")).isEmpty();
        assertThat(evaluator.findSyntaxError("value ((")).isPresent();
    }

    @Test
    void nestSkipsNamesThatClashWithAnExistingValue() {
        Map<String, Object> nested =
                FeelHookEvaluator.nest(
                        Map.of("contact.name", "Ada", "plan", "pro", "plan.tier", "gold"));

        assertThat(nested).containsEntry("contact", Map.of("name", "Ada"));
        assertThat(nested.get("plan")).isIn("pro", Map.of("tier", "gold"));
    }

    private HookOutcome evaluate(String expression, String value) {
        return evaluator.evaluate(expression, value, Map.of(), DEFAULT_ERROR);
    }
}
