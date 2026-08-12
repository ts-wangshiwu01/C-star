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
    // Micronaut core
    annotationProcessor("io.micronaut:micronaut-inject-java")
    implementation("io.micronaut:micronaut-inject")
    implementation("io.micronaut:micronaut-http-server-netty")
    implementation("io.micronaut:micronaut-http-client")

    // gRPC client (to call core-service)
    implementation("io.micronaut.grpc:micronaut-grpc-runtime")

    // JWT validation (SSO)
    implementation("io.micronaut.security:micronaut-security-jwt")

    // Validation
    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("jakarta.validation:jakarta.validation-api")

    // YAML config parsing (application.yml)
    runtimeOnly("org.yaml:snakeyaml")

    // Test
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.assertj:assertj-core")
}
