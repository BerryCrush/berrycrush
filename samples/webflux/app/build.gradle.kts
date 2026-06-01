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

// Configure SpotBugs to exclude sample-specific warnings
tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter.set(file("spotbugs-exclude.xml"))
}

dependencies {
    // Spring Boot WebFlux
    implementation(libs.bundles.spring.boot.webflux)
    runtimeOnly(libs.r2dbc.h2)
    runtimeOnly(libs.h2.database)
}
