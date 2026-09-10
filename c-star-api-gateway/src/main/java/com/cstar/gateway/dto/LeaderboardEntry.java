package com.cstar.gateway.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * Leaderboard row — aggregate received flowers per employee.
 * Mirrors the frontend {@code LeaderboardEntry} (c-star-frontend/src/types/api.ts).
 *
 * @param employeeId  receiver employee id
 * @param displayName receiver display name
 * @param total       total flowers received (within the requested scope)
 * @param rank        1-based rank by {@code total} descending
 */
@Serdeable
public record LeaderboardEntry(
        long employeeId,
        String displayName,
        long total,
        long rank
) {
}