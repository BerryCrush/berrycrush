plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // Spring Boot Web only (no JPA/database)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    
    // Kotlin support
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}
