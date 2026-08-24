package com.cstar.gateway.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * Relative sent-at time as persisted in {@code mock/appreciations-seed.json}
 * ({@code { "daysAgo": N, "hour": H, "minute": M }}). Resolved to an ISO-8601
 * UTC instant at startup, mirroring the frontend {@code atTime} helper.
 *
 * @param daysAgo how many days before "now" the appreciation was sent
 * @param hour    hour of day (0-23)
 * @param minute  minute of hour (0-59)
 */
@Serdeable
public record SeedSentAt(
        int daysAgo,
        int hour,
        int minute
) {
}