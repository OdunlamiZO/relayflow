package io.odunlamizo.relayflow.logging;

// Docs: docs/codebase-components.md#logfmtlayout

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import java.time.Instant;
import java.util.Map;
import net.logstash.logback.marker.SingleFieldAppendingMarker;

public class LogfmtLayout extends LayoutBase<ILoggingEvent> {

    @Override
    public String doLayout(ILoggingEvent event) {
        StringBuilder line = new StringBuilder();
        append(line, "timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
        append(line, "level", event.getLevel().toString());
        append(line, "logger", event.getLoggerName());
        append(line, "thread", event.getThreadName());
        append(line, "msg", StructuredLogEvents.messageWithoutStructuredArguments(event));

        for (Map.Entry<String, String> entry : event.getMDCPropertyMap().entrySet()) {
            append(line, entry.getKey(), entry.getValue());
        }

        for (SingleFieldAppendingMarker argument : StructuredLogEvents.structuredArguments(event)) {
            append(line, argument.getFieldName(), StructuredLogEvents.value(argument));
        }

        IThrowableProxy throwable = event.getThrowableProxy();

        if (throwable != null) {
            append(line, "error.kind", throwable.getClassName());
            append(line, "error.message", throwable.getMessage());
            append(line, "error.stack", ThrowableProxyUtil.asString(throwable));
        }

        return line.append(CoreConstants.LINE_SEPARATOR).toString();
    }

    private static void append(StringBuilder line, String key, String value) {
        if (value == null) {
            return;
        }

        if (!line.isEmpty()) {
            line.append(' ');
        }

        line.append(key).append('=').append(quote(value));
    }

    private static String quote(String value) {
        boolean needsQuotes =
                value.isEmpty()
                        || value.chars()
                                .anyMatch(
                                        character ->
                                                character <= ' '
                                                        || character == '='
                                                        || character == '"');

        if (!needsQuotes) {
            return value;
        }

        String escaped =
                value.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");

        return "\"" + escaped + "\"";
    }
}
