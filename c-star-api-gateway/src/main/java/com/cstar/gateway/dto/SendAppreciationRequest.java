package com.cstar.gateway.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * POST /api/v1/appreciations request — the giver is derived from the caller's
 * identity (JWT), never sent in the body. Mirrors {@code SendAppreciationRequest}
 * (c-star-frontend/src/types/api.ts).
 *
 * @param receiverSsoId SSO id of the receiver
 * @param categoryId    category of the appreciation
 * @param flowerCount   number of red flowers granted (>= 1, <= remaining quota)
 * @param message       free-text message (non-empty)
 */
@Serdeable
public record SendAppreciationRequest(
        String receiverSsoId,
        long categoryId,
        long flowerCount,
        String message
) {
}