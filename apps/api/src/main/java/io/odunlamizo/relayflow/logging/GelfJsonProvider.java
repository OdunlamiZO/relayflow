package io.odunlamizo.relayflow.logging;

// Docs: docs/codebase-components.md#gelfjsonprovider

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import net.logstash.logback.composite.AbstractJsonProvider;
import net.logstash.logback.marker.SingleFieldAppendingMarker;

public class GelfJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        generator.writeStringField("version", "1.1");
        generator.writeStringField("host", getContext().getProperty(CoreConstants.HOSTNAME_KEY));
        generator.writeStringField(
                "short_message", StructuredLogEvents.messageWithoutStructuredArguments(event));
        generator.writeNumberField("timestamp", BigDecimal.valueOf(event.getTimeStamp(), 3));
        generator.writeNumberField("level", syslogLevel(event.getLevel()));
        generator.writeStringField("_logger_name", event.getLoggerName());
        generator.writeStringField("_thread_name", event.getThreadName());

        for (Map.Entry<String, String> entry : event.getMDCPropertyMap().entrySet()) {
            generator.writeStringField("_" + entry.getKey(), entry.getValue());
        }

        for (SingleFieldAppendingMarker argument : StructuredLogEvents.structuredArguments(event)) {
            generator.writeStringField(
                    "_" + argument.getFieldName(), StructuredLogEvents.value(argument));
        }

        IThrowableProxy throwable = event.getThrowableProxy();

        if (throwable != null) {
            generator.writeStringField("full_message", ThrowableProxyUtil.asString(throwable));
            generator.writeStringField("_error_kind", throwable.getClassName());
            generator.writeStringField("_error_message", throwable.getMessage());
        }
    }

    private static int syslogLevel(Level level) {
        return switch (level.toInt()) {
            case Level.ERROR_INT -> 3;
            case Level.WARN_INT -> 4;
            case Level.INFO_INT -> 6;
            default -> 7;
        };
    }
}
