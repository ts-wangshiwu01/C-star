package com.cstar.gateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test — verifies the scaffold compiles and the application class loads.
 */
class ApiGatewayApplicationSmokeTest {

    @Test
    void applicationClassLoads() {
        Class<?> clazz = ApiGatewayApplication.class;
        assertThat(clazz.getSimpleName()).isEqualTo("ApiGatewayApplication");
        assertThat(clazz.getPackage().getName()).isEqualTo("com.cstar.gateway");
    }
}
