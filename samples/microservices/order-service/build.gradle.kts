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

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter.set(file("spotbugs-exclude.xml"))
}

dependencies {
    // Spring Boot
    implementation(libs.bundles.spring.boot)
    implementation(libs.spring.boot.starter.actuator)
    runtimeOnly(libs.h2.database)
}
