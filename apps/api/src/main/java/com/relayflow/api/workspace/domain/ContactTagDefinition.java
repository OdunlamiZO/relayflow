package com.relayflow.api.workspace.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ContactTagDefinition(
        String key, String label, List<String> values, Map<String, String> colors) {

    public static final Set<String> COLORS =
            Set.of("neutral", "blue", "green", "yellow", "orange", "red", "purple", "teal");

    public ContactTagDefinition {
        if (label == null) label = "";
        if (values == null) values = List.of();
        if (colors == null) colors = Map.of();
    }

    public ContactTagDefinition(String key, String label, List<String> values) {
        this(key, label, values, Map.of());
    }
}
