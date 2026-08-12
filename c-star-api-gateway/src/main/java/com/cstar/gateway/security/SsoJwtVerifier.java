package com.cstar.gateway.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * Verifies ALDP SSO JWTs and extracts the claims needed by C-Star.
 *
 * Per detail-design 9527-01-SSO-login.md §2 #1 + §3.2 CK-01~03:
 * <ul>
 *   <li>CK-01: signature must verify against a trusted key; JWT must not be expired</li>
 *   <li>CK-02: {@code sso_id} claim must be present and non-null</li>
 *   <li>CK-03: {@code display_name} claim must be present and non-null</li>
 *   <li>Issuer must match {@code cstar.sso.issuer} from config</li>
 * </ul>
 *
 * <p>Production: trusted key is fetched from {@code cstar.sso.jwks-url} (ALDP JWKS endpoint).
 * For testing: {@link #setTrustedVerificationKey(JWK)} injects a test public key directly,
 * bypassing the JWKS fetch (avoids network dependency in unit tests).</p>
 *
 * <p>Any failure (bad signature, expiry, missing claim, wrong issuer) throws
 * {@link InvalidJwtException}, which the {@code LoginController} maps to
 * {@code SSO_INVALID_JWT (1)}.</p>
 */
@Singleton
public class SsoJwtVerifier {

    private final String expectedIssuer;
    private volatile RSAKey trustedKey;

    /**
     * @param expectedIssuer the ALDP issuer URL (from {@code cstar.sso.issuer}); never null
     */
    public SsoJwtVerifier(@Value("${cstar.sso.issuer}") String expectedIssuer) {
        this.expectedIssuer = Objects.requireNonNull(expectedIssuer, "cstar.sso.issuer must be configured");
    }

    /**
     * Test-only: inject a trusted RSA public key for verifying test-signed JWTs.
     * In production, this verifier would instead fetch keys from the ALDP JWKS URL.
     *
     * @param jwk the RSA JWK (public part) to trust; must not be null
     */
    public void setTrustedVerificationKey(JWK jwk) {
        if (!(jwk instanceof RSAKey rsaKey)) {
            throw new IllegalArgumentException("Only RSA JWKs are supported, got: " + jwk.getKeyType());
        }
        this.trustedKey = rsaKey;
    }

    /**
     * Verify a JWT and extract the SSO claims.
     *
     * @param jwtString the serialized JWT (compact JWS form)
     * @return extracted claims (ssoId + displayName)
     * @throws InvalidJwtException if signature fails, JWT is expired, issuer mismatches,
     *                             or required claims (sso_id, display_name) are missing
     */
    public SsoClaims verify(String jwtString) {
        Objects.requireNonNull(jwtString, "jwt must not be null");

        SignedJWT jwt = parse(jwtString);
        verifySignature(jwt);
        JWTClaimsSet claims = extractClaims(jwt);

        verifyExpiration(claims);
        verifyIssuer(claims);

        String ssoId = requireClaim(claims, "sso_id");
        String displayName = requireClaim(claims, "display_name");

        return new SsoClaims(ssoId, displayName);
    }

    private SignedJWT parse(String jwtString) {
        try {
            return SignedJWT.parse(jwtString);
        } catch (ParseException e) {
            throw new InvalidJwtException("JWT parse failed: " + e.getMessage(), e);
        }
    }

    private void verifySignature(SignedJWT jwt) {
        RSAKey key = this.trustedKey;
        if (key == null) {
            throw new InvalidJwtException("No trusted verification key configured (JWKS fetch not yet implemented)");
        }
        JWSVerifier verifier;
        try {
            verifier = new RSASSAVerifier(key.toRSAPublicKey());
        } catch (JOSEException e) {
            throw new InvalidJwtException("Cannot build RSA verifier from trusted key: " + e.getMessage(), e);
        }
        try {
            if (!jwt.verify(verifier)) {
                throw new InvalidJwtException("JWT signature verification failed");
            }
        } catch (JOSEException e) {
            throw new InvalidJwtException("JWT signature verification error: " + e.getMessage(), e);
        }
    }

    private JWTClaimsSet extractClaims(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new InvalidJwtException("Cannot extract JWT claims: " + e.getMessage(), e);
        }
    }

    private void verifyExpiration(JWTClaimsSet claims) {
        Date expiration = claims.getExpirationTime();
        if (expiration == null) {
            throw new InvalidJwtException("JWT has no expiration time (exp claim missing)");
        }
        if (expiration.toInstant().isBefore(Instant.now())) {
            throw new InvalidJwtException("JWT expired at " + expiration);
        }
    }

    private void verifyIssuer(JWTClaimsSet claims) {
        String issuer = claims.getIssuer();
        if (issuer == null || !issuer.equals(expectedIssuer)) {
            throw new InvalidJwtException(
                    "JWT issuer mismatch: expected '" + expectedIssuer + "', got '" + issuer + "'");
        }
    }

    private String requireClaim(JWTClaimsSet claims, String claimName) {
        Object value = claims.getClaim(claimName);
        if (value == null) {
            throw new InvalidJwtException("JWT missing required claim: " + claimName);
        }
        String str = value.toString();
        if (str.isBlank()) {
            throw new InvalidJwtException("JWT claim " + claimName + " is blank");
        }
        return str;
    }
}
