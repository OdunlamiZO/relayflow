package io.odunlamizo.relayflow.common.dto;

// Docs: docs/codebase-components.md#pageresponse

import java.util.List;

/** nextCursor is null when hasMore is false or the endpoint uses offset pagination. */
public record PageResponse<T>(List<T> items, boolean hasMore, String nextCursor) {}
