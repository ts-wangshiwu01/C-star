package com.cstar.sso;

import com.cstar.sso.proto.SsoLoginErrorCode;
import com.cstar.sso.proto.SsoLoginRequest;
import com.cstar.sso.proto.SsoLoginResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proto contract smoke test — verifies the generated stubs match the contract
 * defined in detail-design 9527-01-SSO-login.md §3.5.
 *
 * Task 1 RED: stubs don't exist yet (proto not written) → compile fails → test fails.
 * Task 1 GREEN: after writing sso_login.proto, Micronaut gRPC plugin generates stubs.
 */
class ProtoContractTest {

    @Test
    void ssoLoginRequestHasExpectedFields() {
        SsoLoginRequest request = SsoLoginRequest.newBuilder()
                .setSsoId("alice")
                .setDisplayName("Alice")
                .build();

        assertThat(request.getSsoId()).isEqualTo("alice");
        assertThat(request.getDisplayName()).isEqualTo("Alice");
    }

    @Test
    void ssoLoginResponseHasExpectedFields() {
        SsoLoginResponse response = SsoLoginResponse.newBuilder()
                .setEmployeeId(1L)
                .setDisplayName("Alice")
                .setIsFirstLogin(true)
                .setErrorCode(SsoLoginErrorCode.SSO_OK)
                .setErrorMessage("")
                .build();

        assertThat(response.getEmployeeId()).isEqualTo(1L);
        assertThat(response.getDisplayName()).isEqualTo("Alice");
        assertThat(response.getIsFirstLogin()).isTrue();
        assertThat(response.getErrorCode()).isEqualTo(SsoLoginErrorCode.SSO_OK);
        assertThat(response.getErrorMessage()).isEmpty();
    }

    @Test
    void ssoLoginErrorCodeHasExpectedValues() {
        // Required values per detail-design §3.5
        assertThat(SsoLoginErrorCode.SSO_OK.getNumber()).isEqualTo(0);
        assertThat(SsoLoginErrorCode.SSO_INVALID_JWT.getNumber()).isEqualTo(1);
        assertThat(SsoLoginErrorCode.SSO_INTERNAL.getNumber()).isEqualTo(2);

        // Gap 3/4 reserved for SSO_RATE_LIMITED / SSO_EMPLOYEE_SUSPENDED (Phase 2)
        // Verify those numbers are NOT assigned in Phase 1
        assertThat(SsoLoginErrorCode.forNumber(3)).isNull();
        assertThat(SsoLoginErrorCode.forNumber(4)).isNull();
    }

    @Test
    void ssoLoginErrorCodeHasExactlyThreeDefinedValuesInPhase1() {
        // Protobuf Java runtime auto-adds UNRECOGNIZED for forward-compat with unknown enum values.
        // We assert only the 3 defined values (excluding UNRECOGNIZED).
        java.util.List<SsoLoginErrorCode> defined = java.util.Arrays.stream(SsoLoginErrorCode.values())
                .filter(v -> v != SsoLoginErrorCode.UNRECOGNIZED)
                .toList();

        assertThat(defined).hasSize(3);
        assertThat(defined).containsExactlyInAnyOrder(
                SsoLoginErrorCode.SSO_OK,
                SsoLoginErrorCode.SSO_INVALID_JWT,
                SsoLoginErrorCode.SSO_INTERNAL
        );
    }
}
