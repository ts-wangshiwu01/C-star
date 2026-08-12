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
            artifact = "io.grpc:protoc-gen-grpc-java:1.65.1"
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
    api("io.grpc:grpc-stub:1.65.1")
    api("io.grpc:grpc-protobuf:1.65.1")
    api("com.google.protobuf:protobuf-java:3.25.5")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}
