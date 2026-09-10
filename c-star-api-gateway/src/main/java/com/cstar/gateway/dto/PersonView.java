package com.cstar.gateway.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * A person as the frontend sees them in an appreciation record (name resolved by backend).
 *
 * @param employeeId  employee primary key
 * @param displayName employee display name
 */
@Serdeable
public record PersonView(
        long employeeId,
        String displayName
) {
}