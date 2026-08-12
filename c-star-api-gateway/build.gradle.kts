plugins {
    id("io.micronaut.application")
}

application {
    mainClass.set("com.cstar.gateway.ApiGatewayApplication")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.cstar.*")
    }
}

dependencies {
    // Shared proto stubs (SsoLoginRpc, etc.)
    implementation(project(":c-star-proto"))

    // Micronaut core
    annotationProcessor("io.micronaut:micronaut-inject-java")
    implementation("io.micronaut:micronaut-inject")
    implementation("io.micronaut:micronaut-http-server-netty")
    implementation("io.micronaut:micronaut-http-client")

    // gRPC client (correct artifact name per Micronaut 4 docs)
    implementation("io.micronaut.grpc:micronaut-grpc-client-runtime")

    // JWT validation (SSO)
    implementation("io.micronaut.security:micronaut-security-jwt")

    // Validation
    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("jakarta.validation:jakarta.validation-api")

    // YAML config parsing
    runtimeOnly("org.yaml:snakeyaml")

    // Test
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.assertj:assertj-core")
}
