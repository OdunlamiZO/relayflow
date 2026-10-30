package io.odunlamizo.relayflow.logging;

// Docs: docs/codebase-components.md#backend-tests

import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LogfmtLayoutTest {

    private final LoggerContext loggerContext = new LoggerContext();

    private final Logger logger = loggerContext.getLogger("io.odunlamizo.relayflow.Example");

    private final LogfmtLayout layout = new LogfmtLayout();

    @Test
    void writesStructuredArgumentsAsTopLevelPairs() {
        LoggingEvent event =
                new LoggingEvent(
                        Logger.class.getName(),
                        logger,
                        Level.INFO,
                        "User logged in {} {}",
                        null,
                        new Object[] {kv("userId", "user-1"), kv("method", "password")});
        event.setMDCPropertyMap(Map.of("requestId", "request-1"));

        String line = layout.doLayout(event);

        assertThat(line)
                .contains("level=INFO")
                .contains("logger=io.odunlamizo.relayflow.Example")
                .contains("msg=\"User logged in\"")
                .contains("requestId=request-1")
                .contains("userId=user-1")
                .contains("method=password")
                .endsWith(System.lineSeparator());
    }

    @Test
    void quotesAndEscapesValuesThatNeedIt() {
        LoggingEvent event =
                new LoggingEvent(
                        Logger.class.getName(),
                        logger,
                        Level.WARN,
                        "Request rejected {}",
                        null,
                        new Object[] {kv("reason", "name: \"must\" not\nbe blank")});
        event.setMDCPropertyMap(Map.of());

        String line = layout.doLayout(event);

        assertThat(line).contains("reason=\"name: \\\"must\\\" not\\nbe blank\"");
        assertThat(line.strip()).doesNotContain("\n");
    }

    @Test
    void addsErrorFieldsForExceptions() {
        LoggingEvent event =
                new LoggingEvent(
                        Logger.class.getName(),
                        logger,
                        Level.ERROR,
                        "Unhandled exception",
                        new IllegalStateException("boom"),
                        null);
        event.setMDCPropertyMap(Map.of());

        String line = layout.doLayout(event);

        assertThat(line)
                .contains("msg=\"Unhandled exception\"")
                .contains("error.kind=java.lang.IllegalStateException")
                .contains("error.message=boom")
                .contains("error.stack=");
    }
}
