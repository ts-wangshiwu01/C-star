package com.cstar.gateway.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * An in-memory employee with SSO id, loaded from {@code mock/employees.json}.
 * Mirrors the frontend {@code MockEmployee} (c-star-frontend/src/api/mock/data.ts).
 *
 * @param employeeId  employee primary key
 * @param displayName display name
 * @param ssoId       SSO id (used to resolve the receiver by {@code ssoId})
 */
@Serdeable
public record MockEmployee(
        long employeeId,
        String displayName,
        String ssoId
) {
}