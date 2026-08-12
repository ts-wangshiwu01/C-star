package com.cstar.gateway.client;

import com.cstar.sso.proto.SsoLoginRequest;
import com.cstar.sso.proto.SsoLoginResponse;
import com.cstar.sso.proto.SsoLoginRpcGrpc;
import jakarta.inject.Singleton;

import java.util.Objects;

/**
 * gRPC client to core-service's {@code SsoLoginRpc}.
 *
 * Per detail-design 9527-01-SSO-login.md §2 #3:
 *   - Maps (ssoId, displayName) → SsoLoginRequest{sso_id, display_name}
 *   - Calls blocking stub's login(request)
 *   - Returns the SsoLoginResponse unchanged (controller decides HTTP status mapping)
 *
 * <p>This class is a thin wrapper around the generated {@link SsoLoginRpcGrpc.SsoLoginRpcBlockingStub}.
 * It does NOT catch gRPC exceptions — network failures (StatusRuntimeException) propagate
 * to the caller, which the LoginController maps to HTTP 500/503.</p>
 */
@Singleton
public class SsoLoginClient {

    private final SsoLoginRpcGrpc.SsoLoginRpcBlockingStub blockingStub;

    public SsoLoginClient(SsoLoginRpcGrpc.SsoLoginRpcBlockingStub blockingStub) {
        this.blockingStub = Objects.requireNonNull(blockingStub, "blockingStub must not be null");
    }

    /**
     * Call core-service SsoLoginRpc.login.
     *
     * @param ssoId       ALDP SSO account (non-null)
     * @param displayName employee display name (non-null)
     * @return the core-service response (may contain error_code != SSO_OK — caller must check)
     * @throws io.grpc.StatusRuntimeException if the gRPC call fails (network, channel down, etc.)
     */
    public SsoLoginResponse login(String ssoId, String displayName) {
        Objects.requireNonNull(ssoId, "ssoId must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");

        SsoLoginRequest request = SsoLoginRequest.newBuilder()
                .setSsoId(ssoId)
                .setDisplayName(displayName)
                .build();

        return blockingStub.login(request);
    }
}
