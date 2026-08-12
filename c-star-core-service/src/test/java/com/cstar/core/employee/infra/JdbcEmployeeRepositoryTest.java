package com.cstar.core.employee.infra;

import com.cstar.core.employee.domain.Employee;
import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task 4 RED: JdbcEmployeeRepository doesn't exist → compile fails → test fails.
 * Task 4 GREEN: after writing @JdbcRepository interface, all tests pass against real MariaDB.
 *
 * Per detail-design 9527-01-SSO-login.md §2 #5:
 *   - findBySsoId(ssoId): Optional<Employee>
 *   - insert(employee): Employee (with generated id)
 *   - updateName(id, name): void
 *   - findById(id): Optional<Employee>
 *
 * Uses @MicronautTest(test) → real MariaDB on localhost:3307 (see application-test.yml).
 * Tests do NOT truncate tables (per user decision: rely on business constraints);
 * ssoId values are unique per test method to avoid collisions.
 */
@MicronautTest(environments = "test")
class JdbcEmployeeRepositoryTest {

    @Inject
    JdbcEmployeeRepository repository;

    @BeforeEach
    void cleanKnownSsoIds() {
        // Best-effort cleanup of ssoIds used by this test class — avoids cross-run collisions
        // without truncating the whole table (other test classes may have data).
        for (String ssoId : new String[]{"alice-t4", "bob-t4", "carol-t4", "dup-t4"}) {
            repository.findBySsoId(ssoId).ifPresent(e -> repository.deleteById(e.id()));
        }
    }

    @Test
    void findBySsoIdReturnsEmptyWhenNotFound() {
        Optional<Employee> found = repository.findBySsoId("nonexistent-t4");
        assertThat(found).isEmpty();
    }

    @Test
    void insertPersistsEmployeeAndReturnsGeneratedId() {
        Employee toInsert = new Employee("alice-t4", "Alice", Instant.parse("2026-08-12T00:00:00Z"), Instant.parse("2026-08-12T00:00:00Z"));

        Employee inserted = repository.save(toInsert);

        assertThat(inserted.id()).isNotNull();
        assertThat(inserted.id()).isPositive();
        assertThat(inserted.ssoId()).isEqualTo("alice-t4");
        assertThat(inserted.name()).isEqualTo("Alice");

        // Verify round-trip via findBySsoId
        Optional<Employee> found = repository.findBySsoId("alice-t4");
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(inserted.id());
        assertThat(found.get().ssoId()).isEqualTo("alice-t4");
        assertThat(found.get().name()).isEqualTo("Alice");
    }

    @Test
    void findByIdReturnsEmployeeWhenExists() {
        Employee inserted = repository.save(
                new Employee("bob-t4", "Bob", Instant.parse("2026-08-12T00:00:00Z"), Instant.parse("2026-08-12T00:00:00Z")));

        Optional<Employee> found = repository.findById(inserted.id());
        assertThat(found).isPresent();
        assertThat(found.get().ssoId()).isEqualTo("bob-t4");
        assertThat(found.get().name()).isEqualTo("Bob");
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        Optional<Employee> found = repository.findById(-99999L);
        assertThat(found).isEmpty();
    }

    @Test
    void updateNameChangesNameWithoutTouchingSsoId() {
        Employee inserted = repository.save(
                new Employee("carol-t4", "Carol", Instant.parse("2026-08-12T00:00:00Z"), Instant.parse("2026-08-12T00:00:00Z")));

        repository.updateName(inserted.id(), "Caroline");

        Optional<Employee> found = repository.findById(inserted.id());
        assertThat(found).isPresent();
        // I2: ssoId must be unchanged
        assertThat(found.get().ssoId()).isEqualTo("carol-t4");
        // Name updated
        assertThat(found.get().name()).isEqualTo("Caroline");
    }

    @Test
    void duplicateSsoIdInsertThrowsDuplicateKeyException_I2() {
        // I2 invariant: uq_employee_sso_id unique constraint must reject duplicates
        repository.save(
                new Employee("dup-t4", "First", Instant.parse("2026-08-12T00:00:00Z"), Instant.parse("2026-08-12T00:00:00Z")));

        // Second save with same ssoId must fail at DB level (wrapped as DataAccessException by Micronaut Data)
        Employee duplicate = new Employee("dup-t4", "Second", Instant.parse("2026-08-12T00:00:00Z"), Instant.parse("2026-08-12T00:00:00Z"));
        assertThatThrownBy(() -> repository.save(duplicate))
                .isInstanceOf(DataAccessException.class);
    }
}
