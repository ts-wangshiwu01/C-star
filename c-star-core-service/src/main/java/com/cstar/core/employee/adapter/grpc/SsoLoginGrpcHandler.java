package com.cstar.core.employee.adapter.grpc;

import com.cstar.core.employee.application.EmployeeSyncService;
import com.cstar.core.employee.application.SyncResult;
import com.cstar.sso.proto.SsoLoginErrorCode;
import com.cstar.sso.proto.SsoLoginRequest;
import com.cstar.sso.proto.SsoLoginResponse;
import com.cstar.sso.proto.SsoLoginRpcGrpc;
import io.grpc.stub.StreamObserver;
import jakarta.inject.Singleton;

/**
 * gRPC adapter for SsoLoginRpc — exposes the SSO login endpoint to api-gateway.
 *
 * Per detail-design 9527-01-SSO-login.md §2 #7 + §3.5:
 *   - Maps SsoLoginRequest{sso_id, display_name} → EmployeeSyncService.sync(ssoId, name)
 *   - Maps SyncResult → SsoLoginResponse{employee_id, display_name, is_first_login, error_code=SSO_OK}
 *   - Catches RuntimeException → SsoLoginResponse{error_code=SSO_INTERNAL, error_message=msg}
 *   - Response DTO contains ONLY: employee_id, display_name, is_first_login, error_code, error_message
 *     (落地页拆分不变量: NO recent_appreciations / ranking fields — those go via HLD §3.1 #5/#6)
 *
 * Extends the gRPC-generated {@link SsoLoginRpcGrpc.SsoLoginRpcImplBase} (Micronaut registers it
 * as a {@code BindableService} bean via {@code @Singleton}).
 */
@Singleton
public class SsoLoginGrpcHandler extends SsoLoginRpcGrpc.SsoLoginRpcImplBase {

    private final EmployeeSyncService syncService;

    public SsoLoginGrpcHandler(EmployeeSyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public void login(SsoLoginRequest request, StreamObserver<SsoLoginResponse> responseObserver) {
        try {
            SyncResult result = syncService.sync(request.getSsoId(), request.getDisplayName());

            SsoLoginResponse response = SsoLoginResponse.newBuilder()
                    .setEmployeeId(result.employeeId())
                    .setDisplayName(result.name())
                    .setIsFirstLogin(result.isFirstLogin())
                    .setErrorCode(SsoLoginErrorCode.SSO_OK)
                    .setErrorMessage("")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            SsoLoginResponse errorResponse = SsoLoginResponse.newBuilder()
                    .setErrorCode(SsoLoginErrorCode.SSO_INTERNAL)
                    .setErrorMessage(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }
}
