plugins {
    id("io.micronaut.application")
}

application {
    mainClass.set("com.cstar.core.CoreServiceApplication")
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
    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")
    implementation("io.micronaut:micronaut-inject")
    implementation("io.micronaut.validation:micronaut-validation")

    // Data JDBC
    annotationProcessor("io.micronaut.data:micronaut-data-processor")
    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")

    // gRPC
    implementation("io.micronaut.grpc:micronaut-grpc-runtime")

    // Flyway migration
    implementation("io.micronaut.flyway:micronaut-flyway")
    runtimeOnly("org.flywaydb:flyway-mysql")

    // Validation + runtime
    implementation("jakarta.validation:jakarta.validation-api")

    // YAML config parsing (application.yml)
    runtimeOnly("org.yaml:snakeyaml")

    // Transaction
    implementation("io.micronaut.data:micronaut-data-tx")

    // Observability (placeholder)
    implementation("io.micronaut.micrometer:micronaut-micrometer-core")

    // Test
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.assertj:assertj-core")

    // Testcontainers for integration tests (MariaDB)
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mariadb")
    testImplementation("org.testcontainers:jdbc")
}
