package com.cstar.core;

import io.micronaut.runtime.Micronaut;

/**
 * Core service entry point.
 *
 * Hosts the appreciation main path: send / list / ranking / quota / SSO lazy sync.
 * Exposes gRPC endpoints (consumed by api-gateway); no direct REST.
 */
public class CoreServiceApplication {

    public static void main(String[] args) {
        Micronaut.run(CoreServiceApplication.class, args);
    }
}
