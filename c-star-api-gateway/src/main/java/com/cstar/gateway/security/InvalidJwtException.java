package com.cstar.gateway.security;

/**
 * Thrown when an SSO JWT fails verification (signature, expiry, missing claims, wrong issuer).
 *
 * Maps to {@code SSO_INVALID_JWT (1)} in {@code SsoLoginErrorCode} per detail-design §3.5.
 */
public class InvalidJwtException extends RuntimeException {

    public InvalidJwtException(String message) {
        super(message);
    }

    public InvalidJwtException(String message, Throwable cause) {
        super(message, cause);
    }
}
