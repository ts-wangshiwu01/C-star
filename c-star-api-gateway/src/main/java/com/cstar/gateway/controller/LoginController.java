package com.cstar.gateway.controller;

import com.cstar.gateway.client.SsoLoginClient;
import com.cstar.gateway.dto.LoginResponse;
import com.cstar.gateway.security.InvalidJwtException;
import com.cstar.gateway.security.SsoClaims;
import com.cstar.gateway.security.SsoJwtVerifier;
import com.cstar.sso.proto.SsoLoginErrorCode;
import com.cstar.sso.proto.SsoLoginResponse;
import io.grpc.StatusRuntimeException;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * REST orchestrator for SSO login.
 *
 * Per detail-design 9527-01-SSO-login.md §2 #5 + §3.5 + §4 (api-gateway POST /api/v1/login):
 * <ol>
 *   <li>extract the Bearer token from the {@code Authorization} header</li>
 *   <li>{@link SsoJwtVerifier#verify(String)} — throws {@link InvalidJwtException} on any failure
 *       (bad signature, expired, missing sso_id/display_name, wrong issuer) → 401
 *       {@code SSO_INVALID_JWT (1)}</li>
 *   <li>{@link SsoLoginClient#login} → core-service; {@link StatusRuntimeException}
 *       (gRPC network/channel failure) propagates → 500 {@code SSO_INTERNAL (2)}</li>
 *   <li>map {@code SsoLoginResponse} to the frontend {@link LoginResponse} when
 *       {@code error_code == SSO_OK}; any other error_code → 500 {@code SSO_INTERNAL (2)}</li>
 * </ol>
 *
 * Errors are returned as 4xx/5xx {@link HttpResponse} with a JSON body
 * {@code {"error_code": <int>, "error_message": "..."}} (detail-design §3.6: the success body
 * carries NO error fields — errors are expressed via HTTP status + error body).
 */
@Singleton
@Controller("/api/v1")
public class LoginController {

    private final SsoJwtVerifier verifier;
    private final SsoLoginClient loginClient;

    public LoginController(SsoJwtVerifier verifier, SsoLoginClient loginClient) {
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
        this.loginClient = Objects.requireNonNull(loginClient, "loginClient must not be null");
    }

    @Post("/login")
    public HttpResponse<?> login(@Header(HttpHeaders.AUTHORIZATION) String authHeader) {
        // 1. Extract the Bearer token.
        final String token;
        try {
            token = extractBearerToken(authHeader);
        } catch (InvalidJwtException e) {
            return invalidJwt(e);
        }

        // 2. Verify the JWT (signature, expiry, issuer, required claims).
        final SsoClaims claims;
        try {
            claims = verifier.verify(token);
        } catch (InvalidJwtException e) {
            return invalidJwt(e);
        }

        // 3. Call core-service. gRPC network/channel failures propagate as 500 SSO_INTERNAL.
        final SsoLoginResponse grpcResp;
        try {
            grpcResp = loginClient.login(claims.ssoId(), claims.displayName());
        } catch (StatusRuntimeException e) {
            return internal(e.getMessage() == null ? "SsoLoginClient.backend unavailable" : e.getMessage());
        }

        // 4. Map error_code → HTTP.
        SsoLoginErrorCode errorCode = grpcResp.getErrorCode();
        if (errorCode != SsoLoginErrorCode.SSO_OK) {
            String message = grpcResp.getErrorMessage();
            if (message == null || message.isBlank()) {
                message = errorCode.name();
            }
            return internal(message);
        }

        return HttpResponse.ok(new LoginResponse(
                grpcResp.getEmployeeId(),
                grpcResp.getDisplayName(),
                grpcResp.getIsFirstLogin()));
    }

    /**
     * Strip the {@code "Bearer "} scheme from the Authorization header.
     *
     * @throws InvalidJwtException if the header is null or not a Bearer token.
     */
    private static String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidJwtException("Missing or malformed Authorization header");
        }
        String token = authHeader.substring("Bearer ".length());
        if (token.isBlank()) {
            throw new InvalidJwtException("Missing or malformed Authorization header");
        }
        return token;
    }

    private static HttpResponse<?> invalidJwt(InvalidJwtException e) {
        return error(HttpStatus.UNAUTHORIZED, SsoLoginErrorCode.SSO_INVALID_JWT, e.getMessage());
    }

    private static HttpResponse<?> internal(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, SsoLoginErrorCode.SSO_INTERNAL, message);
    }

    private static HttpResponse<?> error(HttpStatus status, SsoLoginErrorCode code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error_code", code.getNumber());
        body.put("error_message", message == null ? code.name() : message);
        return HttpResponse.<Map<String, Object>>status(status).body(body);
    }
}