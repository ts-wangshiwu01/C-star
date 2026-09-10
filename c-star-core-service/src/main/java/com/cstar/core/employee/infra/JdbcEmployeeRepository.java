package com.cstar.core.employee.infra;

import com.cstar.core.employee.domain.Employee;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;

/**
 * JDBC repository for the {@code employee} table.
 *
 * Per detail-design 9527-01-SSO-login.md §2 #5:
 *   - {@link #findBySsoId(String)}: lookup by SSO account (used for lazy sync)
 *   - {@link #save(Employee)}: inherited from CrudRepository — persist new employee (returns instance with generated id)
 *   - {@link #updateName(long, String)}: update name only (I4 — SSO rename sync)
 *   - {@link #findById(Long)}: inherited from CrudRepository
 *   - {@link #deleteById(Long)}: inherited, used by test cleanup only
 *
 * Dialect is MYSQL (MariaDB-compatible). Micronaut Data generates the SQL at compile time;
 * duplicate inserts that violate uq_employee_sso_id surface as {@link io.micronaut.data.exceptions.DataAccessException}.
 *
 * Note: {@code Employee} must be {@code @Introspected} for Micronaut Data to map fields ↔ columns.
 */
@JdbcRepository(dialect = Dialect.MYSQL)
public interface JdbcEmployeeRepository extends CrudRepository<Employee, Long> {

    Optional<Employee> findBySsoId(String ssoId);

    /**
     * Updates only the {@code name} column (and {@code updated_at} via ON UPDATE CURRENT_TIMESTAMP).
     * Identity fields (id, sso_id, created_at) are never touched by this method (I2).
     *
     * @param id   employee id
     * @param name new display name from SSO claims
     * @return number of rows affected (1 if found, 0 if not)
     */
    @io.micronaut.data.annotation.Query("UPDATE employee SET name = :name WHERE id = :id")
    int updateName(long id, String name);
}

