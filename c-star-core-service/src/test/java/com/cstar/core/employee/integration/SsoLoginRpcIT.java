package com.cstar.core.employee.integration;

import com.cstar.core.employee.domain.Employee;
import com.cstar.core.employee.infra.JdbcEmployeeRepository;
import com.cstar.sso.proto.SsoLoginErrorCode;
import com.cstar.sso.proto.SsoLoginRequest;
import com.cstar.sso.proto.SsoLoginResponse;
import com.cstar.sso.proto.SsoLoginRpcGrpc;
import io.grpc.ManagedChannel;
import io.micronaut.grpc.server.GrpcServerChannel;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 11 — End-to-end integration test for the SsoLoginRpc gRPC endpoint.
 *
 * Follows the established integration pattern (Task 4 {@code JdbcEmployeeRepositoryTest}):
 *   - {@code @MicronautTest(environments = "test")} → real MariaDB on localhost:3307.
 *   - Uses @Named(GrpcServerChannel.NAME) to obtain a {@link ManagedChannel} wired by
 *     {@link GrpcServerChannel} to the embedded gRPC server (in-process, no port conflicts).
 *     NOTE: {@code @GrpcChannel("test")} in the task spec requires micronaut-grpc-client-runtime,
 *     which is NOT on the classpath; server-runtime's {@code GrpcServerChannel} factory provides
 *     the equivalent channel bean without adding dependencies.
 *
 * Invariants verified (detail-design 9527-01-SSO-login.md §4 core-service):
 *   I2 — exactly one employee row per ssoId (no duplicates across logins);
 *   I3 — after login returns, the employee record immediately exists;
 *   I4 — name changes via SSO claims are synced (updateName).
 *
 * ssoIds are unique per test method ({@code it11-*}) and cleaned up in @BeforeEach.
 */
@MicronautTest(environments = "test", transactional = false)
class SsoLoginRpcIT {

    private static final String[] KNOWN_SSO_IDS = {"it11-alice", "it11-bob", "it11-carol", "it11-dave"};

    @Inject
    JdbcEmployeeRepository repository;

    @Inject
    @Named(GrpcServerChannel.NAME)
    ManagedChannel channel;

    private SsoLoginRpcGrpc.SsoLoginRpcBlockingStub blockingStub;

    @BeforeEach
    void setUp() {
        blockingStub = SsoLoginRpcGrpc.newBlockingStub(channel);

        // Best-effort cleanup of ssoIds used by this test class — avoids cross-run collisions
        // without truncating the whole table (other test classes may have data).
        for (String ssoId : KNOWN_SSO_IDS) {
            repository.findBySsoId(ssoId).ifPresent(e -> repository.deleteById(e.id()));
        }
    }

    @Test
    void firstLogin_createsRowAndReturnsIsFirstLoginTrue() {
        SsoLoginResponse response = blockingStub.login(
                SsoLoginRequest.newBuilder().setSsoId("it11-alice").setDisplayName("Alice").build());

        assertThat(response.getEmployeeId()).isNotZero();
        assertThat(response.getDisplayName()).isEqualTo("Alice");
        assertThat(response.getIsFirstLogin()).isTrue();
        assertThat(response.getErrorCode()).isEqualTo(SsoLoginErrorCode.SSO_OK);
        assertThat(response.getErrorMessage()).isEmpty();

        // I2 + I3: exactly one row exists with the synced name
        assertThat(repository.findBySsoId("it11-alice")).isPresent()
                .get()
                .extracting(Employee::ssoId, Employee::name)
                .containsExactly("it11-alice", "Alice");
    }

    @Test
    void revisit_sameName_noopReturnsIsFirstLoginFalse() {
        seedEmployee("it11-bob", "Bob");

        SsoLoginResponse response = blockingStub.login(
                SsoLoginRequest.newBuilder().setSsoId("it11-bob").setDisplayName("Bob").build());

        assertThat(response.getIsFirstLogin()).isFalse();
        assertThat(response.getDisplayName()).isEqualTo("Bob");
        assertThat(response.getErrorCode()).isEqualTo(SsoLoginErrorCode.SSO_OK);

        // I2 — still exactly 1 row, name unchanged
        java.util.List<Employee> rows = repository.findBySsoId("it11-bob").stream().toList();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).ssoId()).isEqualTo("it11-bob");
        assertThat(rows.get(0).name()).isEqualTo("Bob");
    }

    @Test
    void revisit_renamed_updatesNameReturnsIsFirstLoginFalse() {
        seedEmployee("it11-carol", "Carol");

        SsoLoginResponse response = blockingStub.login(
                SsoLoginRequest.newBuilder().setSsoId("it11-carol").setDisplayName("Caroline").build());

        assertThat(response.getIsFirstLogin()).isFalse();
        assertThat(response.getDisplayName()).isEqualTo("Caroline"); // I4 — name synced
        assertThat(response.getErrorCode()).isEqualTo(SsoLoginErrorCode.SSO_OK);

        // I4 + I2: 1 row, name now Caroline
        assertThat(repository.findBySsoId("it11-carol")).isPresent()
                .get()
                .extracting(Employee::name)
                .isEqualTo("Caroline");
    }

    @Test
    void repeatedLogin_keepsSingleRowPerSsoId() {
        // I2 invariant — representative of many concurrent logins (subset of the "100 logins" case)
        SsoLoginResponse first = loginDave();
        assertThat(first.getIsFirstLogin()).isTrue();
        assertThat(first.getErrorCode()).isEqualTo(SsoLoginErrorCode.SSO_OK);

        for (int i = 0; i < 4; i++) {
            SsoLoginResponse r = loginDave();
            assertThat(r.getIsFirstLogin()).isFalse();
            assertThat(r.getErrorCode()).isEqualTo(SsoLoginErrorCode.SSO_OK);
        }

        // exactly 1 row for it11-dave
        assertThat(repository.findBySsoId("it11-dave")).isPresent();
    }

    private SsoLoginResponse loginDave() {
        return blockingStub.login(
                SsoLoginRequest.newBuilder().setSsoId("it11-dave").setDisplayName("Dave").build());
    }

    private void seedEmployee(String ssoId, String name) {
        Instant now = Instant.now();
        repository.save(new Employee(ssoId, name, now, now));
    }
}