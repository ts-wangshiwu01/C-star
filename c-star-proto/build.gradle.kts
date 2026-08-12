// Shared proto module — generates Java stubs for gRPC services.
// Both c-star-core-service (server) and c-star-api-gateway (client) depend on this module.

plugins {
    id("com.google.protobuf") version "0.9.4"
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

sourceSets {
    main {
        java {
            srcDirs("build/generated/source/proto/main/grpc")
            srcDirs("build/generated/source/proto/main/java")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
    //noinspection GrUnresolvedAccess
    plugins {
        create("grpc") {
            // Must align with grpc-core version pulled by micronaut-grpc-server-runtime:4.4.1 (→ grpc 1.62.2)
            artifact = "io.grpc:protoc-gen-grpc-java:1.62.2"
        }
    }
    generateProtoTasks {
        all().forEach {
            //noinspection GrUnresolvedAccess
            it.plugins {
                create("grpc")
            }
        }
    }
}

dependencies {
    // Align with Micronaut gRPC BOM (micronaut-grpc-server-runtime:4.4.1 → grpc 1.62.2)
    api("io.grpc:grpc-stub:1.62.2")
    api("io.grpc:grpc-protobuf:1.62.2")
    api("com.google.protobuf:protobuf-java:3.25.5")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}
