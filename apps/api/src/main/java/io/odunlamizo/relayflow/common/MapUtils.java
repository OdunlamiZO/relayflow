package io.odunlamizo.relayflow.common;

// Docs: docs/codebase-components.md#maputils

import java.util.LinkedHashMap;
import java.util.Map;

public final class MapUtils {

    private MapUtils() {}

    public static Map<String, Object> copyMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }
}
