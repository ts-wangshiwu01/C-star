package com.cstar.core.employee.application;

import com.cstar.core.employee.domain.Employee;
import com.cstar.core.employee.infra.JdbcEmployeeRepository;
import io.micronaut.data.exceptions.DataAccessException;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Application service that lazily syncs an employee record on SSO login (ADR-0004).
 *
 * Per detail-design 9527-01-SSO-login.md §2 #6 + §3.2 To-Be:
 * <ol>
 *   <li>Lookup employee by ssoId</li>
 *   <li>If absent: insert new employee → SyncResult{isFirstLogin=true}</li>
 *   <li>If present and name unchanged: no-op → SyncResult{isFirstLogin=false}</li>
 *   <li>If present and name differs: updateName → SyncResult{isFirstLogin=false, name=newName}</li>
 * </ol>
 *
 * <p>CK-04 (concurrent first login): if {@code save} throws {@link DataAccessException}
 * due to uq_employee_sso_id unique violation (another concurrent request won the race),
 * the service falls back to {@code findBySsoId} and treats the result as a revisit.
 * Non-duplicate DataAccessExceptions are propagated, not swallowed.</p>
 *
 * <p>Invariants enforced (see detail-design §3.2):
 * I2 — one row per ssoId (delegated to DB unique constraint + CK-04 fallback);
 * I3 — after sync returns, employee record immediately exists;
 * I4 — name changes via SSO claims (updateName called when name differs).</p>
 */
@Singleton
public class EmployeeSyncService {

    private final JdbcEmployeeRepository repository;

    public EmployeeSyncService(JdbcEmployeeRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    /**
     * Lazily sync an employee record.
     *
     * @param ssoId ALDP SSO account (non-null)
     * @param name  display name from SSO claims (non-null)
     * @return sync result with employeeId, name, and isFirstLogin flag
     * @throws NullPointerException if ssoId or name is null
     */
    public SyncResult sync(String ssoId, String name) {
        Objects.requireNonNull(ssoId, "ssoId must not be null");
        Objects.requireNonNull(name, "name must not be null");

        Optional<Employee> existing = repository.findBySsoId(ssoId);
        if (existing.isEmpty()) {
            return insertOrFallback(ssoId, name);
        }
        return syncExisting(existing.get(), name);
    }

    private SyncResult insertOrFallback(String ssoId, String name) {
        Instant now = Instant.now();
        Employee toInsert = new Employee(ssoId, name, now, now);
        try {
            Employee inserted = repository.save(toInsert);
            return new SyncResult(inserted.id(), inserted.name(), true);
        } catch (DataAccessException e) {
            // CK-04: concurrent first login — another request won the race.
            // Only fall back for duplicate-key violations; propagate everything else
            // (connection loss, deadlocks, etc. are not recovery-safe here).
            if (!isDuplicateKey(e)) {
                throw e;
            }
            Optional<Employee> fallback = repository.findBySsoId(ssoId);
            if (fallback.isPresent()) {
                Employee existing = fallback.get();
                // If the winner used a different name, sync that too (idempotent).
                if (!existing.name().equals(name)) {
                    repository.updateName(existing.id(), name);
                    return new SyncResult(existing.id(), name, false);
                }
                return new SyncResult(existing.id(), existing.name(), false);
            }
            throw e;
        }
    }

    /**
     * Heuristic duplicate-key detection — MariaDB JDBC wraps SQLIntegrityConstraintViolationException
     * (SQLSTATE 23000) inside DataAccessException. We check message/cause chain for "Duplicate" or "23000".
     */
    private static boolean isDuplicateKey(DataAccessException e) {
        Throwable cur = e;
        for (int i = 0; i < 5 && cur != null; i++) {
            String msg = cur.getMessage();
            if (msg != null && (msg.contains("Duplicate") || msg.contains("23000"))) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private SyncResult syncExisting(Employee existing, String name) {
        if (existing.name().equals(name)) {
            // I4 — name unchanged: no-op
            return new SyncResult(existing.id(), existing.name(), false);
        }
        // I4 — name changed: updateName
        repository.updateName(existing.id(), name);
        return new SyncResult(existing.id(), name, false);
    }
}
