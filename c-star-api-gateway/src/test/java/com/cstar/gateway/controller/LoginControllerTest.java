package com.cstar.gateway.controller;

import com.cstar.gateway.dto.LoginResponse;
import com.cstar.gateway.security.SsoJwtVerifier;
import com.cstar.sso.proto.SsoLoginErrorCode;
import com.cstar.sso.proto.SsoLoginResponse;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Task 10 RED: LoginController + LoginResponse don't exist → compile fails.
 * Task 10 GREEN: after implementing LoginController + LoginResponse, all tests pass.
 *
 * REST POST /api/v1/login (detail-design 9527-01-SSO-login.md §4 api-gateway table):
 *   - happy first login   → 200 + {employee_id, display_name, is_first_login:true}
 *   - happy revisit       → 200 + {is_first_login:false}
 *   - CK-01 invalid sig   → 401 + error_code=SSO_INVALID_JWT(1)
 *   - CK-01 expired       → 401 + error_code=SSO_INVALID_JWT(1)
 *   - CK-02/03 missing    → 401 + error_code=SSO_INVALID_JWT(1)
 *   - CK-05 SSO_INTERNAL  → 500 + error_code=SSO_INTERNAL(2)
 *   - I1 undefined route  → 404
 *
 * Real HTTP calls to the embedded server. SsoLoginClient is replaced by a @MockBean
 * mock (no real gRPC). SsoJwtVerifier stays real in mock mode; we inject a fresh RSA key
 * as its trusted key and sign test JWTs with it.
 */
// Uses the "test" environment (application-test.yml) to disable Micronaut Security,
// so the controller performs its own JWT verification without the security filter
// intercepting custom-signed test JWTs with 401.
@MicronautTest(environments = "test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Property(name = "cstar.sso.issuer", value = "https://test-sso.example.com")
class LoginControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    SsoJwtVerifier verifier;

    @Inject
    com.cstar.gateway.client.SsoLoginClient loginClient; // the @MockBean mock

    private RSAKey rsaKey;
    private RSASSASigner signer;

    @BeforeAll
    void setupVerifier() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID("test-key-1").generate();
        signer = new RSASSASigner(rsaKey.toPrivateKey());
        // Trust the generated public key so verification happens against our key.
        verifier.setTrustedVerificationKey(rsaKey.toPublicJWK());
    }

    @BeforeEach
    void resetMock() {
        org.mockito.Mockito.reset(loginClient);
    }

    @MockBean(com.cstar.gateway.client.SsoLoginClient.class)
    @Replaces(com.cstar.gateway.client.SsoLoginClient.class)
    com.cstar.gateway.client.SsoLoginClient loginClient() {
        return mock(com.cstar.gateway.client.SsoLoginClient.class);
    }

    @Test
    void happyFirstLogin_returns200AndDto() throws Exception {
        String jwt = signJwt("alice", "Alice");
        when(loginClient.login("alice", "Alice")).thenReturn(SsoLoginResponse.newBuilder()
                .setEmployeeId(1L)
                .setDisplayName("Alice")
                .setIsFirstLogin(true)
                .setErrorCode(SsoLoginErrorCode.SSO_OK)
                .setErrorMessage("")
                .build());

        HttpResponse<LoginResponse> resp = client.toBlocking()
                .exchange(post(jwt), LoginResponse.class);

        assertThat(resp.code()).isEqualTo(HttpStatus.OK.getCode());
        LoginResponse body = resp.body();
        assertThat(body.employeeId()).isEqualTo(1L);
        assertThat(body.displayName()).isEqualTo("Alice");
        assertThat(body.isFirstLogin()).isTrue();
    }

    @Test
    void happyRevisit_returns200_isFirstLoginFalse() throws Exception {
        String jwt = signJwt("bob", "Bob");
        when(loginClient.login("bob", "Bob")).thenReturn(SsoLoginResponse.newBuilder()
                .setEmployeeId(2L)
                .setDisplayName("Bob")
                .setIsFirstLogin(false)
                .setErrorCode(SsoLoginErrorCode.SSO_OK)
                .setErrorMessage("")
                .build());

        HttpResponse<LoginResponse> resp = client.toBlocking()
                .exchange(post(jwt), LoginResponse.class);

        assertThat(resp.code()).isEqualTo(HttpStatus.OK.getCode());
        LoginResponse body = resp.body();
        assertThat(body.employeeId()).isEqualTo(2L);
        assertThat(body.displayName()).isEqualTo("Bob");
        assertThat(body.isFirstLogin()).isFalse();
    }

    @Test
    void ck01_invalidSignature_returns401_ssInvalidJwt() throws Exception {
        // Sign with a different key than the one setTrustedVerificationKey installed.
        RSAKey otherKey = new RSAKeyGenerator(2048).keyID("other-key").generate();
        RSASSASigner otherSigner = new RSASSASigner(otherKey.toPrivateKey());
        String jwt = signJwt(otherSigner, otherKey.getKeyID(), "alice", "Alice");

        HttpResponse<Map<String, Object>> resp = postError(jwt);

        assertThat(resp.code()).isEqualTo(HttpStatus.UNAUTHORIZED.getCode());
        assertThat(resp.body().get("error_code")).isEqualTo(SsoLoginErrorCode.SSO_INVALID_JWT_VALUE);
        verifyNoInteractions(loginClient);
    }

    @Test
    void ck01_expiredJwt_returns401_ssInvalidJwt() throws Exception {
        String jwt = signJwt("alice", "Alice", /* expired */ true);

        HttpResponse<Map<String, Object>> resp = postError(jwt);

        assertThat(resp.code()).isEqualTo(HttpStatus.UNAUTHORIZED.getCode());
        assertThat(resp.body().get("error_code")).isEqualTo(SsoLoginErrorCode.SSO_INVALID_JWT_VALUE);
        verifyNoInteractions(loginClient);
    }

    @Test
    void ck02_03_missingSsoIdClaim_returns401() throws Exception {
        String jwt = signJwt(null, "Alice");

        HttpResponse<Map<String, Object>> resp = postError(jwt);

        assertThat(resp.code()).isEqualTo(HttpStatus.UNAUTHORIZED.getCode());
        assertThat(resp.body().get("error_code")).isEqualTo(SsoLoginErrorCode.SSO_INVALID_JWT_VALUE);
        verifyNoInteractions(loginClient);
    }

    @Test
    void ck05_ssInternal_returns500_ssInternal() throws Exception {
        String jwt = signJwt("carol", "Carol");
        when(loginClient.login("carol", "Carol")).thenReturn(SsoLoginResponse.newBuilder()
                .setErrorCode(SsoLoginErrorCode.SSO_INTERNAL)
                .setErrorMessage("DB down")
                .build());

        HttpResponse<Map<String, Object>> resp = postError(jwt);

        assertThat(resp.code()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
        assertThat(resp.body().get("error_code")).isEqualTo(SsoLoginErrorCode.SSO_INTERNAL_VALUE);
    }

    @Test
    void i1_invariant_registerAndPasswordResetReturn404() {
        // Non-existent routes return 404; toBlocking().exchange() throws for 4xx, so catch.
        int registerCode = statusCodeCatch(HttpRequest.GET("/api/v1/register"));
        assertThat(registerCode).isEqualTo(HttpStatus.NOT_FOUND.getCode());

        int resetCode = statusCodeCatch(HttpRequest.GET("/api/v1/password-reset"));
        assertThat(resetCode).isEqualTo(HttpStatus.NOT_FOUND.getCode());
    }

    private int statusCodeCatch(HttpRequest<?> request) {
        try {
            return client.toBlocking().exchange(request).code();
        } catch (io.micronaut.http.client.exceptions.HttpClientResponseException e) {
            return e.getResponse().code();
        }
    }

    // --- helpers ---

    private MutableHttpRequest<Object> post(String token) {
        return HttpRequest.POST("/api/v1/login", null)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<Map<String, Object>> postError(String token) {
        // Micronaut's toBlocking().exchange() throws HttpClientResponseException for 4xx/5xx.
        // Catch it and extract the response so we can assert on status code + body.
        try {
            return client.toBlocking().exchange(
                    post(token),
                    Argument.mapOf(String.class, Object.class));
        } catch (io.micronaut.http.client.exceptions.HttpClientResponseException e) {
            return (HttpResponse<Map<String, Object>>) (HttpResponse<?>) e.getResponse();
        }
    }

    private String signJwt(String ssoId, String displayName) throws Exception {
        return signJwt(ssoId, displayName, false);
    }

    private String signJwt(String ssoId, String displayName, boolean expired) throws Exception {
        Date now = Date.from(Instant.now().minusSeconds(60));
        Date exp = expired
                ? Date.from(Instant.now().minusSeconds(300))
                : Date.from(Instant.now().plusSeconds(300));
        return signJwt(signer, rsaKey.getKeyID(), ssoId, displayName, now, exp);
    }

    private String signJwt(RSASSASigner withSigner, String kid,
                           String ssoId, String displayName) throws Exception {
        return signJwt(withSigner, kid, ssoId, displayName,
                Date.from(Instant.now().minusSeconds(60)),
                Date.from(Instant.now().plusSeconds(300)));
    }

    private String signJwt(RSASSASigner withSigner, String kid,
                           String ssoId, String displayName,
                           Date issueTime, Date expirationTime) throws Exception {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer("https://test-sso.example.com")
                .issueTime(issueTime)
                .expirationTime(expirationTime);
        if (ssoId != null) builder.claim("sso_id", ssoId);
        if (displayName != null) builder.claim("display_name", displayName);

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).build(),
                builder.build());
        jwt.sign(withSigner);
        return jwt.serialize();
    }
}