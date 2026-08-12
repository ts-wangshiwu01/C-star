package com.cstar.core.employee.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task 3 RED: Employee class doesn't exist → compile fails → test fails.
 * Task 3 GREEN: after writing Employee.java with constructor + withName + field validation, all pass.
 *
 * Per detail-design 9527-01-SSO-login.md §2 #5:
 *   - Employee is a domain entity, immutable sso_id, mutable name (via withName)
 *   - I4: name changes via SSO claims, never locally edited (withName captures this)
 */
class EmployeeTest {

    @Test
    void constructorSetsAllFields() {
        Instant before = Instant.now();
        Employee emp = new Employee(1L, "alice", "Alice", Instant.parse("2026-08-12T00:00:00Z"), Instant.parse("2026-08-12T00:00:00Z"));
        Instant after = Instant.now();

        assertThat(emp.id()).isEqualTo(1L);
        assertThat(emp.ssoId()).isEqualTo("alice");
        assertThat(emp.name()).isEqualTo("Alice");
        assertThat(emp.createdAt()).isEqualTo(Instant.parse("2026-08-12T00:00:00Z"));
        assertThat(emp.updatedAt()).isEqualTo(Instant.parse("2026-08-12T00:00:00Z"));
    }

    @Test
    void withNameReturnsNewInstanceWithUpdatedName() {
        Employee original = new Employee(1L, "alice", "Alice", Instant.parse("2026-08-12T00:00:00Z"), Instant.parse("2026-08-12T00:00:00Z"));
        Employee renamed = original.withName("Alice Smith");

        // I4: name changes via withName; original instance unchanged (immutable)
        assertThat(original.name()).isEqualTo("Alice");
        assertThat(renamed.name()).isEqualTo("Alice Smith");

        // Other fields preserved
        assertThat(renamed.id()).isEqualTo(1L);
        assertThat(renamed.ssoId()).isEqualTo("alice");
        assertThat(renamed.createdAt()).isEqualTo(Instant.parse("2026-08-12T00:00:00Z"));
        // updatedAt should advance (or at least not regress) — we don't pin it, just check non-null
        assertThat(renamed.updatedAt()).isNotNull();
    }

    @Test
    void withNamePreservesIdentityFields() {
        Employee original = new Employee(42L, "bob", "Bob", Instant.parse("2026-08-12T00:00:00Z"), Instant.parse("2026-08-12T00:00:00Z"));
        Employee renamed = original.withName("Robert");

        // id and ssoId must never change (I2: one row per sso_id; id is immutable PK)
        assertThat(renamed.id()).isEqualTo(42L);
        assertThat(renamed.ssoId()).isEqualTo("bob");
        assertThat(renamed.createdAt()).isEqualTo(original.createdAt());
    }

    @Test
    void nullSsoIdRejected() {
        assertThatThrownBy(() ->
                new Employee(1L, null, "Alice", Instant.now(), Instant.now())
        ).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ssoId");
    }

    @Test
    void nullNameRejected() {
        assertThatThrownBy(() ->
                new Employee(1L, "alice", null, Instant.now(), Instant.now())
        ).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");
    }

    @Test
    void withNameNullRejected() {
        Employee original = new Employee(1L, "alice", "Alice", Instant.now(), Instant.now());
        assertThatThrownBy(() -> original.withName(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");
    }
}
