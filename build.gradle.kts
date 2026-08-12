// Root build file — declares plugins for subprojects to apply, common repos only.
// Each subproject applies its own plugin (io.micronaut.application) and own deps.

plugins {
    id("io.micronaut.application") version "4.4.2" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    group = "com.cstar"
    version = "0.1.0"
}
