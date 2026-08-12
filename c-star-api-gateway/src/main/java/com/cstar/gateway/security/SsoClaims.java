package com.cstar.gateway.security;

/**
 * Extracted SSO claims from a verified ALDP JWT.
 *
 * @param ssoId       ALDP SSO account (unique identifier for the employee)
 * @param displayName Employee display name (from SSO profile, may change over time — I4)
 */
public record SsoClaims(
        String ssoId,
        String displayName
) {
}
