package com.cstar.core.employee.infra;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration test for V0__create_employee.sql.
 *
 * Task 2 RED: V0 migration doesn't exist yet → Flyway baseline fails or table missing → test fails.
 * Task 2 GREEN: after writing V0__create_employee.sql, Flyway runs it and the table is present.
 *
 * Uses Testcontainers MariaDB via application-test.yml.
 */
@MicronautTest(environments = "test")
class EmployeeTableMigrationTest {

    @Inject
    DataSource dataSource;

    @Test
    void employeeTableExists() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "employee", new String[]{"TABLE"})) {
                assertThat(rs.next()).as("employee table must exist after migration").isTrue();
            }
        }
    }

    @Test
    void employeeTableHasExpectedColumns() throws Exception {
        List<String> columnNames = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "employee", null)) {
                while (rs.next()) {
                    columnNames.add(rs.getString("COLUMN_NAME"));
                }
            }
        }
        assertThat(columnNames).containsExactlyInAnyOrder(
                "id", "sso_id", "name", "created_at", "updated_at"
        );
    }

    @Test
    void employeeTableDoesNotHaveOrgChartFields_I5() throws Exception {
        // I5 invariant: employee table must NOT contain team / manager / employment_status columns
        List<String> columnNames = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "employee", null)) {
                while (rs.next()) {
                    columnNames.add(rs.getString("COLUMN_NAME"));
                }
            }
        }
        assertThat(columnNames)
                .doesNotContain("team", "manager", "manager_id", "employment_status", "department");
    }

    @Test
    void employeeTableHasUniqueIndexOnSsoId_I2() throws Exception {
        // I2 invariant: uq_employee_sso_id unique constraint must exist
        boolean foundUnique = false;
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getIndexInfo(null, null, "employee", true, false)) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    if ("uq_employee_sso_id".equalsIgnoreCase(indexName)) {
                        foundUnique = true;
                        break;
                    }
                }
            }
        }
        assertThat(foundUnique)
                .as("uq_employee_sso_id unique index must exist (I2 invariant)")
                .isTrue();
    }
}
