package com.cstar.gateway.integration;

import com.cstar.gateway.client.SsoLoginClientFactory;
import com.cstar.sso.proto.SsoLoginRpcGrpc;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;

import jakarta.inject.Singleton;

/**
 * Test bean override that points the gateway's {@code SsoLoginClient} gRPC stub at the in-process
 * {@link FakeCoreServer} instead of a real core-service over the network.
 *
 * <p><b>Why {@code @Primary}:</b> the production {@link SsoLoginClientFactory} still exposes a
 * {@code SsoLoginRpcBlockingStub} bean in normal contexts, and reflection-driven bean discovery in a
 * test context can leave BOTH the production and this test stub registered — which makes
 * {@code SsoLoginClient} injection ambiguous ("Multiple possible bean candidates"). Marking this stub
 * {@link Primary} forces Micronaut to always prefer it, so {@code SsoLoginClient} resolves
 * unambiguously to the stub that talks to {@link FakeCoreServer}.</p>
 */
@Factory
class GrpcTestClientConfiguration {

    @Bean
    @Primary
    @Singleton
    SsoLoginRpcGrpc.SsoLoginRpcBlockingStub ssoLoginBlockingStub() {
        FakeCoreServer.start();
        return SsoLoginRpcGrpc.newBlockingStub(FakeCoreServer.channel());
    }
}