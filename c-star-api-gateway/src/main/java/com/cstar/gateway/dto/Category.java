package com.cstar.gateway.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * A preset appreciation category. Mirrors the frontend {@code Category} type
 * (c-star-frontend/src/types/api.ts + categories-data.ts).
 *
 * @param id          category primary key
 * @param name        category display name
 * @param description short helper shown under the category in the picker
 */
@Serdeable
public record Category(
        long id,
        String name,
        String description
) {
}