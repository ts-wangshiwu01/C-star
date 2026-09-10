package com.cstar.gateway.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * Seed appreciation row loaded from {@code mock/appreciations-seed.json}.
 * Giver/receiver are referenced by employee id and resolved to {@link PersonView}
 * at startup; {@code sentAt} is a relative {@link SeedSentAt}.
 *
 * @param id          appreciation primary key
 * @param giverId      employee id of the giver
 * @param receiverId   employee id of the receiver
 * @param categoryId   category of the appreciation
 * @param flowerCount  number of red flowers granted
 * @param message      free-text message
 * @param sentAt       relative sent-at time (see {@link SeedSentAt})
 */
@Serdeable
public record SeedAppreciation(
        long id,
        long giverId,
        long receiverId,
        long categoryId,
        long flowerCount,
        String message,
        SeedSentAt sentAt
) {
}