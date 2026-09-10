package com.cstar.gateway.client;

import com.cstar.sso.proto.SsoLoginRequest;
import com.cstar.sso.proto.SsoLoginResponse;
import com.cstar.sso.proto.SsoLoginRpcGrpc;
import io.micronaut.context.annotation.Factory;
import io.micronaut.grpc.annotation.GrpcChannel;
import io.grpc.ManagedChannel;
import jakarta.inject.Singleton;

/**
 * Factory bean that exposes a {@link SsoLoginRpcGrpc.SsoLoginRpcBlockingStub} for injection.
 *
 * Micronaut gRPC does not auto-create client stub beans; per the official guide
 * (https://micronaut-projects.github.io/micronaut-grpc/latest/guide/#client), you must
 * expose the stub via a @Factory. The @GrpcChannel value "cstar-core" references
 * the {@code grpc.channels.cstar-core} config block in application.yml
 * (host + port of core-service).
 *
 * Task 12 (LoginFlowIT) will override this bean with an in-process channel for integration tests.
 */
@Factory
public class SsoLoginClientFactory {

    @Singleton
    public SsoLoginRpcGrpc.SsoLoginRpcBlockingStub ssoLoginBlockingStub(
            @GrpcChannel("cstar-core") ManagedChannel channel) {
        return SsoLoginRpcGrpc.newBlockingStub(channel);
    }
}
