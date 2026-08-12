package com.cstar.gateway.integration;

import com.cstar.sso.proto.SsoLoginErrorCode;
import com.cstar.sso.proto.SsoLoginRequest;
import com.cstar.sso.proto.SsoLoginResponse;
import com.cstar.sso.proto.SsoLoginRpcGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test stand-in for the core-service gRPC endpoint (SsoLoginRpc) used by {@link LoginFlowIT}.
 *
 * <p>Starts a real in-process netty gRPC server on an ephemeral port and exposes connect info to the
 * gateway test. The handler reproduces core-service's {@code EmployeeSyncService.sync} contract:
 * first login → new row + {@code is_first_login=true}; revisit same name → same row, false; renamed
 * → same id, updated name, false (I4). State is held in memory (no MariaDB) and reset per test via
 * {@link #resetState()}.</p>
 */
final class FakeCoreServer {

    private static final FakeSsoGrpcHandler HANDLER = new FakeSsoGrpcHandler();
    private static Server server;
    private static io.grpc.ManagedChannel channel;

    private FakeCoreServer() {
    }

    /** Lazily start the in-process gRPC server and open a plaintext channel to it. */
    static synchronized void start() {
        if (server == null) server = startServer();
    }

    private static Server startServer() {
        try {
            server = ServerBuilder.forPort(0)
                    .addService(HANDLER)
                    .build()
                    .start();
            channel = io.grpc.ManagedChannelBuilder
                    .forAddress("127.0.0.1", server.getPort())
                    .usePlaintext()
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start in-process gRPC server", e);
        }
        return server;
    }

    /** @return the managed channel to the in-process server (caller owns shutdown). */
    static io.grpc.ManagedChannel channel() {
        return channel;
    }

    static void resetState() {
        HANDLER.reset();
    }

    static synchronized void stop() {
        if (server != null) {
            server.shutdownNow();
            try {
                server.awaitTermination();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            server = null;
        }
        if (channel != null) {
            channel.shutdownNow();
            channel = null;
        }
    }

    /** gRPC service implementation mirroring the core-service sync contract (in-memory). */
    static final class FakeSsoGrpcHandler extends SsoLoginRpcGrpc.SsoLoginRpcImplBase {
        private final AtomicLong seq = new AtomicLong();
        private final ConcurrentHashMap<String, Row> rows = new ConcurrentHashMap<>();

        void reset() {
            rows.clear();
        }

        @Override
        public void login(SsoLoginRequest request, StreamObserver<SsoLoginResponse> observer) {
            String ssoId = request.getSsoId();
            String name = request.getDisplayName();

            Row inserted = rows.putIfAbsent(ssoId, new Row(seq.incrementAndGet(), name));
            if (inserted == null) {
                Row stored = rows.get(ssoId);
                observer.onNext(ok(stored.id, stored.name, true));
                observer.onCompleted();
                return;
            }

            // Revisit — keep the same id; I4: sync a renamed display_name.
            boolean renamed = !inserted.name.equals(name);
            if (renamed) {
                rows.put(ssoId, new Row(inserted.id, name));
            }
            observer.onNext(ok(inserted.id, name, false));
            observer.onCompleted();
        }

        private static SsoLoginResponse ok(long id, String name, boolean first) {
            return SsoLoginResponse.newBuilder()
                    .setEmployeeId(id)
                    .setDisplayName(name)
                    .setIsFirstLogin(first)
                    .setErrorCode(SsoLoginErrorCode.SSO_OK)
                    .setErrorMessage("")
                    .build();
        }

        private record Row(long id, String name) {
        }
    }
}