package com.cstar.gateway.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/**
 * Generic paginated result. Mirrors the frontend {@code PageResult} (c-star-frontend/src/types/api.ts).
 *
 * @param items the page's items (sorted/paginated by the service)
 * @param page  1-based page number
 * @param size  page size
 * @param total total number of items matching the query (unfiltered by paging)
 * @param <T>   item type
 */
@Serdeable
public record PageResult<T>(
        List<T> items,
        long page,
        long size,
        long total
) {
}