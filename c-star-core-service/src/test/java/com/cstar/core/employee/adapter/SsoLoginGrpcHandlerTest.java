package com.cstar.core.employee.adapter;

import com.cstar.core.employee.application.EmployeeSyncService;
import com.cstar.core.employee.application.SyncResult;
import com.cstar.core.employee.adapter.grpc.SsoLoginGrpcHandler;
import com.cstar.sso.proto.SsoLoginErrorCode;
import com.cstar.sso.proto.SsoLoginRequest;
import com.cstar.sso.proto.SsoLoginResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Task 6 RED: SsoLoginGrpcHandler doesn't exist → compile fails → test fails.
 * Task 6 GREEN: after writing SsoLoginGrpcHandler extends SsoLoginRpcGrpc.SsoLoginRpcImplBase,
 *   all tests pass with mocked EmployeeSyncService.
 *
 * Per detail-design 9527-01-SSO-login.md §2 #7 + §3.5:
 *   - DTO mapping: SsoLoginRequest{sso_id, display_name} → EmployeeSyncService.sync(ssoId, name)
 *   - Response: SsoLoginResponse{employee_id, display_name, is_first_login, error_code, error_message}
 *   - Happy path → error_code=SSO_OK
 *   - sync throws RuntimeException → error_code=SSO_INTERNAL + error_message
 *   - Response DTO must NOT contain recent_appreciations / ranking fields (拆分不变量)
 *
 * Uses @ExtendWith(MockitoExtension.class) — pure unit test, no Micronaut context, no DB.
 */
@ExtendWith(MockitoExtension.class)
class SsoLoginGrpcHandlerTest {

    @Mock
    EmployeeSyncService syncService;

    @InjectMocks
    SsoLoginGrpcHandler handler;

    @Test
    void happyPath_firstLogin_returnsOkWithIsFirstLoginTrue() {
        SsoLoginRequest request = SsoLoginRequest.newBuilder()
                .setSsoId("alice")
                .setDisplayName("Alice")
                .build();
        when(syncService.sync("alice", "Alice"))
                .thenReturn(new SyncResult(1L, "Alice", true));

        SsoLoginResponse response = sendSync(request);

        assertThat(response.getEmployeeId()).isEqualTo(1L);
        assertThat(response.getDisplayName()).isEqualTo("Alice");
        assertThat(response.getIsFirstLogin()).isTrue();
        assertThat(response.getErrorCode()).isEqualTo(SsoLoginErrorCode.SSO_OK);
        assertThat(response.getErrorMessage()).isEmpty();
    }

    @Test
    void happyPath_revisitNameChanged_returnsOkWithNewNameAndIsFirstLoginFalse() {
        SsoLoginRequest request = SsoLoginRequest.newBuilder()
                .setSsoId("carol")
                .setDisplayName("Caroline")
                .build();
        when(syncService.sync("carol", "Caroline"))
                .thenReturn(new SyncResult(99L, "Caroline", false));

        SsoLoginResponse response = sendSync(request);

        assertThat(response.getEmployeeId()).isEqualTo(99L);
        assertThat(response.getDisplayName()).isEqualTo("Caroline");  // I4: new name
        assertThat(response.getIsFirstLogin()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo(SsoLoginErrorCode.SSO_OK);
    }

    @Test
    void happyPath_revisitNameSame_returnsOkWithIsFirstLoginFalse() {
        SsoLoginRequest request = SsoLoginRequest.newBuilder()
                .setSsoId("bob")
                .setDisplayName("Bob")
                .build();
        when(syncService.sync("bob", "Bob"))
                .thenReturn(new SyncResult(42L, "Bob", false));

        SsoLoginResponse response = sendSync(request);

        assertThat(response.getEmployeeId()).isEqualTo(42L);
        assertThat(response.getDisplayName()).isEqualTo("Bob");
        assertThat(response.getIsFirstLogin()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo(SsoLoginErrorCode.SSO_OK);
    }

    @Test
    void syncThrowsRuntimeException_returnsInternalError() {
        SsoLoginRequest request = SsoLoginRequest.newBuilder()
                .setSsoId("dave")
                .setDisplayName("Dave")
                .build();
        when(syncService.sync("dave", "Dave"))
                .thenThrow(new RuntimeException("DB connection lost"));

        SsoLoginResponse response = sendSync(request);

        assertThat(response.getErrorCode()).isEqualTo(SsoLoginErrorCode.SSO_INTERNAL);
        assertThat(response.getErrorMessage()).contains("DB connection lost");
        // employee_id and display_name are zero/empty on error
        assertThat(response.getEmployeeId()).isZero();
        assertThat(response.getDisplayName()).isEmpty();
        assertThat(response.getIsFirstLogin()).isFalse();
    }

    @Test
    void responseDtoDoesNotContainLandingPageFields() {
        // 落地页拆分不变量: Response DTO fields = {employee_id, display_name, is_first_login, error_code, error_message}
        // Must NOT contain recent_appreciations or ranking fields
        SsoLoginRequest request = SsoLoginRequest.newBuilder()
                .setSsoId("alice")
                .setDisplayName("Alice")
                .build();
        when(syncService.sync(any(), any()))
                .thenReturn(new SyncResult(1L, "Alice", true));

        SsoLoginResponse response = sendSync(request);

        // Verify only the 5 expected fields are populated; the proto descriptor ensures
        // no recent_appreciations / ranking fields exist (they're not in the proto).
        // Here we just assert the descriptor field count matches the contract.
        assertThat(response.getDescriptorForType().getFields())
                .hasSize(5);

        java.util.List<String> fieldNames = response.getDescriptorForType().getFields().stream()
                .map(f -> f.getName())
                .toList();
        assertThat(fieldNames).containsExactlyInAnyOrder(
                "employee_id", "display_name", "is_first_login", "error_code", "error_message"
        );
        assertThat(fieldNames).doesNotContain("recent_appreciations", "ranking");
    }

    /**
     * Helper: invoke the handler's login method synchronously and return the captured response.
     */
    private SsoLoginResponse sendSync(SsoLoginRequest request) {
        @SuppressWarnings("unchecked")
        StreamObserver<SsoLoginResponse> observer = mock(StreamObserver.class);
        ArgumentCaptor<SsoLoginResponse> captor = ArgumentCaptor.forClass(SsoLoginResponse.class);

        handler.login(request, observer);

        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        verify(observer, never()).onError(any());
        return captor.getValue();
    }
}
