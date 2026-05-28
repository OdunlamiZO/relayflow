package com.relayflow.api.messaging.dto;

import java.util.List;

/**
 * Generic paginated response returned by list endpoints.
 *
 * <p>{@code nextCursor} is a stable, opaque string the client passes back as a query parameter to
 * fetch the next page. It is {@code null} when {@code hasMore} is {@code false} or when the
 * endpoint uses offset-based pagination and does not issue a cursor.
 */
public record PageResponse<T>(List<T> items, boolean hasMore, String nextCursor) {}
