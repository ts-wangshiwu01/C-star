package com.cstar.core.employee.application;

/**
 * Result of an employee sync operation.
 *
 * @param employeeId   local employee.id (never null after sync)
 * @param name         display name after sync (input on first login; possibly updated on revisit)
 * @param isFirstLogin true = created this call; false = already existed
 */
public record SyncResult(
        Long employeeId,
        String name,
        boolean isFirstLogin
) {
}
