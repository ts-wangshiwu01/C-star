package com.cstar.gateway.client;

import com.cstar.sso.proto.SsoLoginErrorCode;
import com.cstar.sso.proto.SsoLoginRequest;
import com.cstar.sso.proto.SsoLoginResponse;
import com.cstar.sso.proto.SsoLoginRpcGrpc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Task 9 RED: SsoLoginClient doesn't exist → compile fails → test fails.
 * Task 9 GREEN: after writing SsoLoginClient + @Factory stub bean, all tests pass.
 *
 * Per detail-design 9527-01-SSO-login.md §2 #3:
 *   - login(ssoId, displayName): SsoLoginResponse — calls core-service gRPC SsoLoginRpc.login
 *   - DTO mapping: (ssoId, displayName) → SsoLoginRequest{sso_id, display_name}
 *
 * Unit test strategy: mock the gRPC blocking stub, verify SsoLoginClient:
 *   1. Builds SsoLoginRequest with correct field mapping
 *   2. Calls stub.login(request)
 *   3. Returns the stub's SsoLoginResponse unchanged
 *
 * Real gRPC round-trip (channel wiring, @GrpcChannel injection) is covered by Task 12
 * integration test (LoginFlowIT) which spins up both services.
 */
@ExtendWith(MockitoExtension.class)
class SsoLoginClientTest {

    @Mock
    SsoLoginRpcGrpc.SsoLoginRpcBlockingStub blockingStub;

    @InjectMocks
    SsoLoginClient client;

    @Test
    void loginMapsDtoFieldsAndReturnsResponse() {
        // Given: stub returns a happy-path response
        SsoLoginResponse stubResponse = SsoLoginResponse.newBuilder()
                .setEmployeeId(1L)
                .setDisplayName("Alice")
                .setIsFirstLogin(true)
                .setErrorCode(SsoLoginErrorCode.SSO_OK)
                .setErrorMessage("")
                .build();
        when(blockingStub.login(any(SsoLoginRequest.class))).thenReturn(stubResponse);

        // When
        SsoLoginResponse response = client.login("alice", "Alice");

        // Then: response propagated unchanged
        assertThat(response).isSameAs(stubResponse);
        assertThat(response.getEmployeeId()).isEqualTo(1L);
        assertThat(response.getDisplayName()).isEqualTo("Alice");
        assertThat(response.getIsFirstLogin()).isTrue();
        assertThat(response.getErrorCode()).isEqualTo(SsoLoginErrorCode.SSO_OK);

        // And: request DTO mapping verified
        ArgumentCaptor<SsoLoginRequest> captor = ArgumentCaptor.forClass(SsoLoginRequest.class);
        verify(blockingStub).login(captor.capture());
        SsoLoginRequest sentRequest = captor.getValue();
        assertThat(sentRequest.getSsoId()).isEqualTo("alice");
        assertThat(sentRequest.getDisplayName()).isEqualTo("Alice");
    }

    @Test
    void loginPropagatesInternalErrorResponse() {
        // Given: stub returns an error response (core-service hit an internal error)
        SsoLoginResponse errorResponse = SsoLoginResponse.newBuilder()
                .setErrorCode(SsoLoginErrorCode.SSO_INTERNAL)
                .setErrorMessage("DB connection lost")
                .build();
        when(blockingStub.login(any(SsoLoginRequest.class))).thenReturn(errorResponse);

        // When
        SsoLoginResponse response = client.login("dave", "Dave");

        // Then: error propagated as-is (gateway decides how to map to HTTP status in LoginController)
        assertThat(response.getErrorCode()).isEqualTo(SsoLoginErrorCode.SSO_INTERNAL);
        assertThat(response.getErrorMessage()).isEqualTo("DB connection lost");
        assertThat(response.getEmployeeId()).isZero();
        assertThat(response.getIsFirstLogin()).isFalse();
    }

    @Test
    void loginPropagatesStubRuntimeException() {
        // Given: stub throws (network failure, channel shutdown, etc.)
        when(blockingStub.login(any(SsoLoginRequest.class)))
                .thenThrow(new io.grpc.StatusRuntimeException(io.grpc.Status.UNAVAILABLE));

        // When/Then: SsoLoginClient does not swallow gRPC exceptions — propagates to caller
        try {
            client.login("eve", "Eve");
            org.assertj.core.api.Assertions.fail("Expected StatusRuntimeException to propagate");
        } catch (io.grpc.StatusRuntimeException e) {
            assertThat(e.getStatus()).isEqualTo(io.grpc.Status.UNAVAILABLE);
        }
    }
}
