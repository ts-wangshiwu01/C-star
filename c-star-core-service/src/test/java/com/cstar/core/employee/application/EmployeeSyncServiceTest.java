package com.cstar.core.employee.application;

import com.cstar.core.employee.domain.Employee;
import com.cstar.core.employee.infra.JdbcEmployeeRepository;
import io.micronaut.data.exceptions.DataAccessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Task 5 RED: EmployeeSyncService doesn't exist → compile fails → test fails.
 * Task 5 GREEN: after writing EmployeeSyncService + SyncResult, all tests pass with mocked repo.
 *
 * Per detail-design 9527-01-SSO-login.md §2 #6 + §3.2 To-Be:
 *   sync(ssoId, name): SyncResult
 *     - not exists → insert → SyncResult{employeeId, name, isFirstLogin=true}
 *     - exists + name same → no-op → SyncResult{employeeId, name, isFirstLogin=false}
 *     - exists + name different → updateName → SyncResult{employeeId, newName, isFirstLogin=false}
 *     - CK-04: insert撞DuplicateKeyException → 转查findBySsoId → 视为回访
 *
 * Mock-only tests (no DB): @MicronautTest + @MockBean for the repository.
 * Real-DB integration is covered by Task 11 (SsoLoginRpcIT).
 */
@ExtendWith(MockitoExtension.class)
class EmployeeSyncServiceTest {

    @Mock
    JdbcEmployeeRepository repository;

    @InjectMocks
    EmployeeSyncService syncService;

    @Test
    void syncCreatesEmployeeWhenNotExists() {
        // Given: repository returns empty for "alice"
        when(repository.findBySsoId("alice")).thenReturn(Optional.empty());
        Employee inserted = new Employee(1L, "alice", "Alice", Instant.parse("2026-08-12T00:00:00Z"), Instant.parse("2026-08-12T00:00:00Z"));
        when(repository.save(any(Employee.class))).thenReturn(inserted);

        // When
        SyncResult result = syncService.sync("alice", "Alice");

        // Then
        assertThat(result.employeeId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Alice");
        assertThat(result.isFirstLogin()).isTrue();

        verify(repository).findBySsoId("alice");
        verify(repository).save(any(Employee.class));
        verify(repository, never()).updateName(anyLong(), anyString());
    }

    @Test
    void syncReturnsExistingWhenNameSame() {
        // Given: existing employee with same name
        Employee existing = new Employee(42L, "bob", "Bob", Instant.parse("2026-08-12T00:00:00Z"), Instant.parse("2026-08-12T00:00:00Z"));
        when(repository.findBySsoId("bob")).thenReturn(Optional.of(existing));

        // When
        SyncResult result = syncService.sync("bob", "Bob");

        // Then: no insert, no update, just return
        assertThat(result.employeeId()).isEqualTo(42L);
        assertThat(result.name()).isEqualTo("Bob");
        assertThat(result.isFirstLogin()).isFalse();

        verify(repository).findBySsoId("bob");
        verify(repository, never()).save(any(Employee.class));
        verify(repository, never()).updateName(anyLong(), anyString());
    }

    @Test
    void syncUpdatesNameWhenChanged_I4() {
        // Given: existing employee, name differs
        Employee existing = new Employee(99L, "carol", "Carol", Instant.parse("2026-08-12T00:00:00Z"), Instant.parse("2026-08-12T00:00:00Z"));
        when(repository.findBySsoId("carol")).thenReturn(Optional.of(existing));
        when(repository.updateName(99L, "Caroline")).thenReturn(1);

        // When
        SyncResult result = syncService.sync("carol", "Caroline");

        // Then: I4 — name updated, returned name is the NEW name (not the old one)
        assertThat(result.employeeId()).isEqualTo(99L);
        assertThat(result.name()).isEqualTo("Caroline");
        assertThat(result.isFirstLogin()).isFalse();

        verify(repository).findBySsoId("carol");
        verify(repository, never()).save(any(Employee.class));
        verify(repository).updateName(99L, "Caroline");
    }

    @Test
    void syncHandlesConcurrentFirstLoginDuplicateKeyException_CK04() {
        // Given: findBySsoId empty, but save throws DuplicateKeyException (concurrent first login)
        when(repository.findBySsoId("dave")).thenReturn(Optional.empty())
                .thenReturn(Optional.of(new Employee(7L, "dave", "Dave", Instant.parse("2026-08-12T00:00:00Z"), Instant.parse("2026-08-12T00:00:00Z"))));
        when(repository.save(any(Employee.class))).thenThrow(new DataAccessException("Duplicate key"));

        // When
        SyncResult result = syncService.sync("dave", "Dave");

        // Then: CK-04 — duplicate exception caught, fall back to findBySsoId, treated as revisit
        assertThat(result.employeeId()).isEqualTo(7L);
        assertThat(result.name()).isEqualTo("Dave");
        assertThat(result.isFirstLogin()).isFalse();

        verify(repository, times(2)).findBySsoId("dave");  // initial + fallback
        verify(repository).save(any(Employee.class));      // attempted but failed
        verify(repository, never()).updateName(anyLong(), anyString());
    }

    @Test
    void syncPropagatesNonDuplicateDataAccessException() {
        // Given: findBySsoId empty, save throws non-duplicate DataAccessException (e.g. connection lost)
        when(repository.findBySsoId("eve")).thenReturn(Optional.empty());
        when(repository.save(any(Employee.class))).thenThrow(new DataAccessException("Connection lost"));

        // When/Then: non-duplicate exception should propagate (not be swallowed)
        try {
            syncService.sync("eve", "Eve");
            org.assertj.core.api.Assertions.fail("Expected DataAccessException to propagate");
        } catch (DataAccessException e) {
            assertThat(e.getMessage()).contains("Connection lost");
        }

        verify(repository).findBySsoId("eve");
        verify(repository).save(any(Employee.class));
        // No second findBySsoId call (only duplicate-key triggers fallback)
        verify(repository, times(1)).findBySsoId("eve");
    }
}
