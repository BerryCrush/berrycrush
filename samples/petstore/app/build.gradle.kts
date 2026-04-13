plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // Spring Boot
    implementation(libs.bundles.spring.boot)
    runtimeOnly(libs.h2.database)
}
