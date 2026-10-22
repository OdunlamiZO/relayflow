package com.relayflow.api.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import net.logstash.logback.marker.SingleFieldAppendingMarker;

final class StructuredLogEvents {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\s*\\{}");

    private StructuredLogEvents() {}

    static List<SingleFieldAppendingMarker> structuredArguments(ILoggingEvent event) {
        Object[] arguments = event.getArgumentArray();

        if (arguments == null) {
            return List.of();
        }

        return Arrays.stream(arguments)
                .filter(SingleFieldAppendingMarker.class::isInstance)
                .map(SingleFieldAppendingMarker.class::cast)
                .toList();
    }

    static String messageWithoutStructuredArguments(ILoggingEvent event) {
        Object[] arguments = event.getArgumentArray();

        if (arguments == null
                || !Arrays.stream(arguments)
                        .allMatch(SingleFieldAppendingMarker.class::isInstance)) {
            return event.getFormattedMessage();
        }

        return PLACEHOLDER.matcher(event.getMessage()).replaceAll("").trim();
    }

    static String value(SingleFieldAppendingMarker argument) {
        return argument.toStringSelf().substring(argument.getFieldName().length() + 1);
    }
}
