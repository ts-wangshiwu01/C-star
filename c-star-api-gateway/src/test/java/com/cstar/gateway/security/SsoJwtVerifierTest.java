package com.cstar.gateway.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task 8 RED: SsoJwtVerifier doesn't exist → compile fails → test fails.
 * Task 8 GREEN: after writing SsoJwtVerifier + SsoClaims + InvalidJwtException, all tests pass.
 *
 * Per detail-design 9527-01-SSO-login.md §2 #1 + §3.2 CK-01~03:
 *   - verify(jwt): SsoClaims — extracts sso_id + display_name
 *   - CK-01: invalid signature → InvalidJwtException (→ SSO_INVALID_JWT=1)
 *   - CK-01: expired JWT → InvalidJwtException
 *   - CK-02: missing sso_id claim → InvalidJwtException
 *   - CK-03: missing display_name claim → InvalidJwtException
 *
 * Strategy: generate an RSA key pair at class setup, expose its JWKS as the verifier's
 * trusted key (test-only — production uses ALDP JWKS URL).
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Property(name = "cstar.sso.issuer", value = "https://test-sso.example.com")
class SsoJwtVerifierTest {

    @Inject
    SsoJwtVerifier verifier;

    private RSAKey rsaKey;
    private RSASSASigner signer;

    @BeforeAll
    void setup() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID("test-key-1").generate();
        signer = new RSASSASigner(rsaKey.toPrivateKey());
        // Inject the test public key into the verifier so it can verify our signed JWTs.
        verifier.setTrustedVerificationKey(rsaKey.toPublicJWK());
    }

    @Test
    void happyPath_extractsSsoIdAndDisplayName() throws Exception {
        String jwt = signJwt("alice", "Alice", "https://test-sso.example.com",
                Date.from(Instant.now().minusSeconds(60)),
                Date.from(Instant.now().plusSeconds(300)));

        SsoClaims claims = verifier.verify(jwt);

        assertThat(claims.ssoId()).isEqualTo("alice");
        assertThat(claims.displayName()).isEqualTo("Alice");
    }

    @Test
    void ck01_invalidSignature_throwsInvalidJwtException() throws Exception {
        // Sign with a different key — verifier will reject
        RSAKey otherKey = new RSAKeyGenerator(2048).keyID("other-key").generate();
        RSASSASigner otherSigner = new RSASSASigner(otherKey.toPrivateKey());

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("other-key").build(),
                new JWTClaimsSet.Builder()
                        .issuer("https://test-sso.example.com")
                        .claim("sso_id", "alice")
                        .claim("display_name", "Alice")
                        .issueTime(Date.from(Instant.now().minusSeconds(60)))
                        .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                        .build());
        jwt.sign(otherSigner);

        assertThatThrownBy(() -> verifier.verify(jwt.serialize()))
                .isInstanceOf(InvalidJwtException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void ck01_expiredJwt_throwsInvalidJwtException() throws Exception {
        String jwt = signJwt("alice", "Alice", "https://test-sso.example.com",
                Date.from(Instant.now().minusSeconds(600)),
                Date.from(Instant.now().minusSeconds(300)));  // expired 5 min ago

        assertThatThrownBy(() -> verifier.verify(jwt))
                .isInstanceOf(InvalidJwtException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void ck02_missingSsoIdClaim_throwsInvalidJwtException() throws Exception {
        String jwt = signJwt(null, "Alice", "https://test-sso.example.com",
                Date.from(Instant.now().minusSeconds(60)),
                Date.from(Instant.now().plusSeconds(300)));

        assertThatThrownBy(() -> verifier.verify(jwt))
                .isInstanceOf(InvalidJwtException.class)
                .hasMessageContaining("sso_id");
    }

    @Test
    void ck03_missingDisplayNameClaim_throwsInvalidJwtException() throws Exception {
        String jwt = signJwt("alice", null, "https://test-sso.example.com",
                Date.from(Instant.now().minusSeconds(60)),
                Date.from(Instant.now().plusSeconds(300)));

        assertThatThrownBy(() -> verifier.verify(jwt))
                .isInstanceOf(InvalidJwtException.class)
                .hasMessageContaining("display_name");
    }

    @Test
    void wrongIssuer_throwsInvalidJwtException() throws Exception {
        String jwt = signJwt("alice", "Alice", "https://wrong-issuer.example.com",
                Date.from(Instant.now().minusSeconds(60)),
                Date.from(Instant.now().plusSeconds(300)));

        assertThatThrownBy(() -> verifier.verify(jwt))
                .isInstanceOf(InvalidJwtException.class)
                .hasMessageContaining("issuer");
    }

    /**
     * Helper: sign a JWT with the trusted test key.
     * null values for ssoId/displayName skip the claim (to test missing-claim rejection).
     */
    private String signJwt(String ssoId, String displayName, String issuer,
                           Date issueTime, Date expirationTime) throws Exception {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .issueTime(issueTime)
                .expirationTime(expirationTime);
        if (ssoId != null) builder.claim("sso_id", ssoId);
        if (displayName != null) builder.claim("display_name", displayName);

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                builder.build());
        jwt.sign(signer);
        return jwt.serialize();
    }
}
