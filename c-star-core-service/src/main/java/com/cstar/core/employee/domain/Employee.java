package com.cstar.core.employee.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Employee domain entity — local record lazily created on first SSO login (ADR-0004).
 *
 * Invariants (see detail-design 9527-01-SSO-login.md §3.2):
 *   - id: immutable PK (set once on insert)
 *   - ssoId: immutable (I2 — one row per sso_id, never changes)
 *   - name: mutable via {@link #withName(String)} only (I4 — name comes from SSO claims,
 *           never edited locally; SSO rename → next login syncs via withName)
 *   - createdAt: immutable (set once on insert)
 *   - updatedAt: advances on every withName call
 *
 * This is an immutable value object: {@link #withName(String)} returns a new instance
 * rather than mutating this one. Repository translates the new instance into an UPDATE.
 */
public final class Employee {

    private final long id;
    private final String ssoId;
    private final String name;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Employee(long id, String ssoId, String name, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ssoId = Objects.requireNonNull(ssoId, "ssoId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public long id() {
        return id;
    }

    public String ssoId() {
        return ssoId;
    }

    public String name() {
        return name;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    /**
     * Returns a new Employee with the name replaced and updatedAt advanced to now.
     * Identity fields (id, ssoId, createdAt) are preserved.
     *
     * @throws NullPointerException if name is null
     */
    public Employee withName(String name) {
        Objects.requireNonNull(name, "name must not be null");
        return new Employee(this.id, this.ssoId, name, this.createdAt, Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee employee)) return false;
        return id == employee.id
                && Objects.equals(ssoId, employee.ssoId)
                && Objects.equals(name, employee.name)
                && Objects.equals(createdAt, employee.createdAt)
                && Objects.equals(updatedAt, employee.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ssoId, name, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "Employee{id=" + id
                + ", ssoId='" + ssoId + '\''
                + ", name='" + name + '\''
                + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt
                + '}';
    }
}
