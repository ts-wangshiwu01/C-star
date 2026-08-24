package com.cstar.gateway.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * One public appreciation record (feed / sent / received lists).
 * Mirrors the frontend {@code AppreciationView} type (c-star-frontend/src/types/api.ts).
 *
 * @param id          appreciation primary key
 * @param giver       the employee who sent the flower
 * @param receiver    the employee who received the flower
 * @param categoryId  category of the appreciation
 * @param flowerCount number of red flowers granted
 * @param message     free-text message
 * @param sentAt      ISO-8601 timestamp (UTC, Beijing-time semantics)
 */
@Serdeable
public record AppreciationView(
        long id,
        PersonView giver,
        PersonView receiver,
        long categoryId,
        long flowerCount,
        String message,
        String sentAt
) {
}