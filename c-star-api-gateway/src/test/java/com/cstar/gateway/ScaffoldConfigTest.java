package com.cstar.gateway;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 7 RED: api-gateway config doesn't yet have JWKS settings + proto dep → assertions fail.
 * Task 7 GREEN: after updating build.gradle.kts + application.yml, all assertions pass.
 *
 * Verifies the scaffold wiring (no business logic) — ensures:
 *   - api-gateway has c-star-proto on its classpath (so SsoLoginRpc stubs are available)
 *   - application.yml declares cstar.sso.jwks-url + cstar.sso.issuer
 *   - application.yml declares cstar-core.grpc.host + cstar-core.grpc.port (for gRPC client)
 *
 * No @MicronautTest (avoids starting full HTTP server just to read config).
 * Loads just the Environment via ApplicationContext.run().
 */
class ScaffoldConfigTest {

    @Test
    void applicationConfigHasSsoJwksUrl() {
        try (ApplicationContext ctx = ApplicationContext.run(Environment.TEST)) {
            Optional<String> jwksUrl = ctx.getProperty("cstar.sso.jwks-url", String.class);
            assertThat(jwksUrl)
                    .as("cstar.sso.jwks-url must be configured (ALDP JWKS endpoint for JWT verify)")
                    .isPresent();
        }
    }

    @Test
    void applicationConfigHasSsoIssuer() {
        try (ApplicationContext ctx = ApplicationContext.run(Environment.TEST)) {
            Optional<String> issuer = ctx.getProperty("cstar.sso.issuer", String.class);
            assertThat(issuer)
                    .as("cstar.sso.issuer must be configured (ALDP issuer for JWT verify)")
                    .isPresent();
        }
    }

    @Test
    void applicationConfigHasCoreGrpcHostAndPort() {
        try (ApplicationContext ctx = ApplicationContext.run(Environment.TEST)) {
            Optional<String> host = ctx.getProperty("cstar-core.grpc.host", String.class);
            Optional<Integer> port = ctx.getProperty("cstar-core.grpc.port", Integer.class);

            assertThat(host)
                    .as("cstar-core.grpc.host must be configured (gRPC client → core-service)")
                    .isPresent();
            assertThat(port)
                    .as("cstar-core.grpc.port must be configured (gRPC client → core-service)")
                    .isPresent();
        }
    }

    @Test
    void protoStubClassesAreOnClasspath() throws ClassNotFoundException {
        // Verifies the c-star-proto module dependency is wired into api-gateway's compile classpath.
        // If build.gradle.kts is missing implementation(project(":c-star-proto")), this throws.
        Class.forName("com.cstar.sso.proto.SsoLoginRequest");
        Class.forName("com.cstar.sso.proto.SsoLoginResponse");
        Class.forName("com.cstar.sso.proto.SsoLoginErrorCode");
        Class.forName("com.cstar.sso.proto.SsoLoginRpcGrpc");
    }
}
