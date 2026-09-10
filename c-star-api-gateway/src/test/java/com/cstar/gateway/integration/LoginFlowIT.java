package com.cstar.gateway.integration;

import com.cstar.gateway.dto.LoginResponse;
import com.cstar.gateway.security.SsoJwtVerifier;
import com.cstar.sso.proto.SsoLoginErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 12 — End-to-end integration test for the API gateway's SSO login flow.
 *
 * <p><b>Approach: B (in-process real gRPC server; gateway's real {@code SsoLoginClient}).</b></p>
 *
 * <p>This test drives the gateway end to end: {@code POST /api/v1/login} with a real Bearer JWT → the
 * gateway's {@link SsoJwtVerifier} verifies it → {@code SsoLoginClient} issues a <b>real gRPC
 * request over the wire (netty, plaintext)</b> → a live in-process gRPC server handles it → the
 * gateway maps the grpc {@code SsoLoginResponse} into the {@link LoginResponse} DTO and returns it.
 * So the gateway's real {@code SsoLoginClient} (stub invocation, field mapping, error mapping) is
 * exercised for real; only the HTTP/2 socket server implementation is provided by the test instead
 * of the core-service process.</p>
 *
 * <p><b>Invariant coverage (detail-design 9527-01-SSO-login.md):</b>
 * <ol>
 *   <li>I1 — the gateway exposes only the login route: {@code POST /api/v1/register} → 404.</li>
 *   <li>Landing-page split (拆分不变量) — the login success body is the compact {@code LoginResponse}
 *       only; it contains NO recent_appreciations / ranking / landing fields.</li>
 * </ol>
 *
 * <p><b>Why B and not A (real core-service handler):</b> Approach A would add
 * {@code testImplementation(project(":c-star-core-service"))} to the gateway's build, pulling
 * core-service's gRPC server-runtime, JDBC/MariaDB datasource, Flyway and Data-repository beans
 * into the <i>gateway</i> test context — inviting bean-conflict / context-startup hazards for
 * marginal extra coverage, because the core-service business logic (first-login / revisit / rename /
 * concurrent first login) is <b>already</b> verified by Task 11 ({@code SsoLoginRpcIT}). The focus
 * here is the gateway flow (HTTP → JWT verify → gRPC call → response mapping), so the gRPC server is
 * backed by a behavioral fake that reproduces the core-service first-login/revisit/rename contract
 * (the {@code FakeSsoGrpcHandler} held by {@link FakeCoreServer}) and keeps the gateway test module
 * isolated (no MariaDB needed).</p>
 *
 * <p><b>Bean override:</b> the package-level {@code GrpcTestClientConfiguration} {@code @Factory}
 * exposes a {@link com.cstar.sso.proto.SsoLoginRpcGrpc.SsoLoginRpcBlockingStub} marked
 * {@link io.micronaut.context.annotation.Primary}, so the gateway's {@code SsoLoginClient} resolves
 * unambiguously to a stub bound to {@link FakeCoreServer}, the in-process plaintext server of the
 * fake handler.</p>
 *
 * <p><b>Local-only test resource:</b> {@code environments = "test"} disables Micronaut Security via
 * {@code src/test/resources/application-test.yml}. That file is <b>gitignored</b> (see .gitignore)
 * and is NOT committed — a developer must have it present locally for this test to run (same pattern
 * as core-service's MariaDB integration tests on :3307).</p>
 */
@MicronautTest(environments = "test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Property(name = "cstar.sso.issuer", value = "https://test-sso.example.com")
class LoginFlowIT {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    SsoJwtVerifier verifier;

    private RSAKey rsaKey;
    private RSASSASigner signer;

    @BeforeAll
    void setupJwtKey() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID("it12-key-1").generate();
        signer = new RSASSASigner(rsaKey.toPrivateKey());
        // Trust OUR generated key so the gateway verifies the test-signed JWTs (mock mode override).
        verifier.setTrustedVerificationKey(rsaKey.toPublicJWK());
    }

    @AfterAll
    static void tearDown() {
        FakeCoreServer.stop();
    }

    @BeforeEach
    void resetCoreState() {
        // Fresh in-memory store per test so first-login semantics are deterministic per test.
        FakeCoreServer.resetState();
    }

    // ---------- Test cases ----------

    @Test
    void firstLogin_withValidJwt_returns200AndNewEmployee() throws Exception {
        HttpResponse<LoginResponse> resp = client.toBlocking()
                .exchange(post(jwt("it12-alice", "Alice")), LoginResponse.class);

        assertThat(resp.code()).isEqualTo(HttpStatus.OK.getCode());
        LoginResponse body = resp.body();
        assertThat(body.employeeId()).isNotZero();
        assertThat(body.displayName()).isEqualTo("Alice");
        assertThat(body.isFirstLogin()).isTrue();
    }

    @Test
    void revisit_sameName_returnsIsFirstLoginFalse() throws Exception {
        String token = jwt("it12-alice", "Alice");

        HttpResponse<LoginResponse> first = client.toBlocking()
                .exchange(post(token), LoginResponse.class);
        assertThat(first.body().isFirstLogin()).isTrue();

        HttpResponse<LoginResponse> second = client.toBlocking()
                .exchange(post(token), LoginResponse.class);
        assertThat(second.body().employeeId()).isEqualTo(first.body().employeeId());
        assertThat(second.body().displayName()).isEqualTo("Alice");
        assertThat(second.body().isFirstLogin()).isFalse();
    }

    @Test
    void revisit_renamed_updatesName_returnsNotFirstLogin() throws Exception {
        client.toBlocking().exchange(post(jwt("it12-alice", "Alice")), LoginResponse.class);

        HttpResponse<LoginResponse> renamed = client.toBlocking()
                .exchange(post(jwt("it12-alice", "Alice2")), LoginResponse.class);
        assertThat(renamed.body().isFirstLogin()).isFalse();
        assertThat(renamed.body().displayName()).isEqualTo("Alice2");
    }

    @Test
    void ck01_invalidSignature_returns401_ssInvalidJwt() throws Exception {
        RSAKey otherKey = new RSAKeyGenerator(2048).keyID("it12-other").generate();
        RSASSASigner otherSigner = new RSASSASigner(otherKey.toPrivateKey());
        String token = signJwt(otherSigner, otherKey.getKeyID(), "it12-alice", "Alice");

        HttpResponse<Map<String, Object>> resp = postError(token);
        assertThat(resp.code()).isEqualTo(HttpStatus.UNAUTHORIZED.getCode());
        assertThat(resp.body().get("error_code")).isEqualTo(SsoLoginErrorCode.SSO_INVALID_JWT_VALUE);
    }

    @Test
    void ck01_expiredJwt_returns401_ssInvalidJwt() throws Exception {
        HttpResponse<Map<String, Object>> resp = postError(signJwt("it12-alice", "Alice", true));
        assertThat(resp.code()).isEqualTo(HttpStatus.UNAUTHORIZED.getCode());
        assertThat(resp.body().get("error_code")).isEqualTo(SsoLoginErrorCode.SSO_INVALID_JWT_VALUE);
    }

    @Test
    void i1_invariant_registerReturns404() {
        int registerCode = statusCode(HttpRequest.POST("/api/v1/register", null));
        assertThat(registerCode).isEqualTo(HttpStatus.NOT_FOUND.getCode());
    }

    @Test
    void landingPageSplit_invariant_loginBodyHasNoLandingFields() throws Exception {
        HttpResponse<String> resp = client.toBlocking()
                .exchange(post(jwt("it12-alice", "Alice")), String.class);
        assertThat(resp.code()).isEqualTo(HttpStatus.OK.getCode());
        String body = resp.body();

        // 拆分不变量: login response is the compact LoginResponse only — no landing-page fields.
        assertThat(body).doesNotContain("recent_appreciations")
                .doesNotContain("ranking")
                .doesNotContain("landing");
    }

    // ---------- helpers ----------

    private int statusCode(HttpRequest<?> request) {
        try {
            return client.toBlocking().exchange(request, Object.class).code();
        } catch (HttpClientResponseException e) {
            // Micronaut throws for non-2xx; the status of the response is what we want to assert.
            return e.getResponse().code();
        }
    }

    private MutableHttpRequest<Object> post(String token) {
        return HttpRequest.POST("/api/v1/login", null)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<Map<String, Object>> postError(String token) {
        try {
            return client.toBlocking().exchange(
                    post(token),
                    Argument.mapOf(String.class, Object.class));
        } catch (HttpClientResponseException e) {
            return (HttpResponse<Map<String, Object>>) (HttpResponse<?>) e.getResponse();
        }
    }

    private String jwt(String ssoId, String displayName) throws Exception {
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