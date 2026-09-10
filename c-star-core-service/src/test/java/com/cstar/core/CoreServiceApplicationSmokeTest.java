package com.cstar.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test — verifies the scaffold compiles and the application class loads.
 * Real TDD tests live alongside each task in the 9527-01-send-appreciation task-list.
 */
class CoreServiceApplicationSmokeTest {

    @Test
    void applicationClassLoads() {
        Class<?> clazz = CoreServiceApplication.class;
        assertThat(clazz.getSimpleName()).isEqualTo("CoreServiceApplication");
        assertThat(clazz.getPackage().getName()).isEqualTo("com.cstar.core");
    }
}
