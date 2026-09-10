package com.cstar.gateway.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * POST /api/v1/appreciations success body. Mirrors {@code SendAppreciationResult}
 * (c-star-frontend/src/types/api.ts).
 *
 * @param appreciationId  id of the newly created appreciation
 * @param remainingQuota remaining quota after this send
 */
@Serdeable
public record SendAppreciationResult(
        long appreciationId,
        long remainingQuota
) {
}