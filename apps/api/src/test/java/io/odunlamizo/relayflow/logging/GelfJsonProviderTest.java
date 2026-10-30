package io.odunlamizo.relayflow.logging;

// Docs: docs/codebase-components.md#backend-tests

import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;
import org.junit.jupiter.api.Test;

class GelfJsonProviderTest {

    private final LoggerContext loggerContext = new LoggerContext();

    private final Logger logger = loggerContext.getLogger("io.odunlamizo.relayflow.Example");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writesGelfFieldsWithPrefixedAdditionalFields() throws Exception {
        LoggingEvent event =
                new LoggingEvent(
                        Logger.class.getName(),
                        logger,
                        Level.WARN,
                        "Webhook delivery rejected {}",
                        null,
                        new Object[] {kv("statusCode", 500)});
        event.setTimeStamp(1_700_000_000_123L);
        event.setMDCPropertyMap(Map.of("requestId", "request-1"));

        JsonNode json = encode(event);

        assertThat(json.get("version").asText()).isEqualTo("1.1");
        assertThat(json.get("host").asText()).isNotBlank();
        assertThat(json.get("short_message").asText()).isEqualTo("Webhook delivery rejected");
        assertThat(json.get("timestamp").decimalValue()).isEqualByComparingTo("1700000000.123");
        assertThat(json.get("level").asInt()).isEqualTo(4);
        assertThat(json.get("_logger_name").asText()).isEqualTo("io.odunlamizo.relayflow.Example");
        assertThat(json.get("_requestId").asText()).isEqualTo("request-1");
        assertThat(json.get("_statusCode").asText()).isEqualTo("500");
        assertThat(json.has("full_message")).isFalse();
    }

    @Test
    void writesStackTraceAsFullMessage() throws Exception {
        LoggingEvent event =
                new LoggingEvent(
                        Logger.class.getName(),
                        logger,
                        Level.ERROR,
                        "Unhandled exception",
                        new IllegalStateException("boom"),
                        null);
        event.setMDCPropertyMap(Map.of());

        JsonNode json = encode(event);

        assertThat(json.get("level").asInt()).isEqualTo(3);
        assertThat(json.get("full_message").asText()).contains("IllegalStateException: boom");
        assertThat(json.get("_error_kind").asText()).isEqualTo("java.lang.IllegalStateException");
        assertThat(json.get("_error_message").asText()).isEqualTo("boom");
    }

    private JsonNode encode(LoggingEvent event) throws Exception {
        LoggingEventCompositeJsonEncoder encoder = new LoggingEventCompositeJsonEncoder();
        encoder.setContext(loggerContext);
        encoder.getProviders().addProvider(new GelfJsonProvider());
        encoder.start();

        return objectMapper.readTree(encoder.encode(event));
    }
}
