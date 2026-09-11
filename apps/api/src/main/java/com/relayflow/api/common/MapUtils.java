package com.relayflow.api.common;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MapUtils {

    private MapUtils() {}

    /** Returns a mutable copy of {@code source}, or an empty map if {@code source} is null. */
    public static Map<String, Object> copyMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }
}
