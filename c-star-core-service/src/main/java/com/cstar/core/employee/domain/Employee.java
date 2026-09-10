package com.cstar.core.employee.domain;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;

import java.time.Instant;
import java.util.Objects;

/**
 * Employee domain entity — local record lazily created on first SSO login (ADR-0004).
 *
 * Implemented as a Java {@code record} so that:
 *   - All fields are inherently final (immutability enforced by language)
 *   - Micronaut Data JDBC can introspect and map fields ↔ columns cleanly
 *   - Accessors ({@code id()}, {@code ssoId()}, {@code name()}, etc.) are auto-generated
 *
 * Invariants (see detail-design 9527-01-SSO-login.md §3.2):
 *   - id: immutable PK (set once on insert; repository populates it on save via @GeneratedValue)
 *   - ssoId: immutable (I2 — one row per sso_id, never changes)
 *   - name: mutable only via {@link #withName(String)} (I4 — name from SSO claims, never edited locally)
 *   - createdAt: immutable (set once on insert)
 *   - updatedAt: advances on every withName call
 *
 * Field name mapping (camelCase → snake_case by default): id→id, ssoId→sso_id, name→name,
 * createdAt→created_at, updatedAt→updated_at. The DDL in V0__create_employee.sql matches.
 */
@MappedEntity
public record Employee(
        @GeneratedValue @Id Long id,
        String ssoId,
        String name,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Compact constructor — validates non-null constraints before record instantiation.
     * id is allowed to be null (sentinel for "not yet persisted"; repository populates real id on save).
     */
    public Employee {
        Objects.requireNonNull(ssoId, "ssoId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * Convenience constructor for the not-yet-persisted case (id is null).
     * Used by application layer when creating a new Employee for insert.
     */
    public Employee(String ssoId, String name, Instant createdAt, Instant updatedAt) {
        this(null, ssoId, name, createdAt, updatedAt);
    }

    /**
     * Returns a new Employee with the name replaced and updatedAt advanced to now.
     * Identity fields (id, ssoId, createdAt) are preserved — never mutated in place.
     *
     * @param name new display name from SSO claims
     * @return new Employee instance; this instance is unchanged
     * @throws NullPointerException if name is null
     */
    public Employee withName(String name) {
        Objects.requireNonNull(name, "name must not be null");
        return new Employee(this.id, this.ssoId, name, this.createdAt, Instant.now());
    }
}
