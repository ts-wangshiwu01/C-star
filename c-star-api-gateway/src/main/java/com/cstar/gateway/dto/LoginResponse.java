package com.cstar.gateway.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * Login-success DTO returned to the frontend.
 *
 * Per detail-design 9527-01-SSO-login.md §3.6, the success body field set is
 * {@code {employee_id, display_name, is_first_login}} — NO error_code/error_message
 * in the success body (errors are expressed via HTTP status + error body instead).
 *
 * @param employeeId   employee primary key from core-service ({@code SsoLoginResponse.employee_id})
 * @param displayName  employee display name from core-service
 * @param isFirstLogin whether this is the employee's first login (drives frontend onboarding)
 */
@Serdeable
public record LoginResponse(
        long employeeId,
        String displayName,
        boolean isFirstLogin
) {
}