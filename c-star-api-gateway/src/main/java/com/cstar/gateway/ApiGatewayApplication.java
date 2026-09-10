package com.cstar.gateway;

import io.micronaut.runtime.Micronaut;

/**
 * API Gateway entry point.
 *
 * Responsibilities: SSO JWT verify, REST→gRPC routing, protocol conversion.
 * No business logic; forwards to core-service (Phase 1) / admin-service (Phase 2).
 */
public class ApiGatewayApplication {

    public static void main(String[] args) {
        Micronaut.run(ApiGatewayApplication.class, args);
    }
}
