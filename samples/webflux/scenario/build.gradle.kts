plugins {
    java
    kotlin("jvm")
    id("berrycrush.test-config")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // Test against the webflux application
    testImplementation(project(":samples:webflux:app"))

    // BerryCrush JUnit integration
    testImplementation(project(":berrycrush:core"))
    testImplementation(project(":berrycrush:junit"))
    testImplementation(project(":berrycrush:spring"))

    // JUnit
    testImplementation(libs.bundles.junit)
    testImplementation(libs.junit.platform.suite.api)
    testImplementation(libs.junit.platform.launcher)

    // Spring Boot test
    testImplementation(libs.spring.boot.starter.test)
}

tasks.test {
    useJUnitPlatform {
        includeEngines("berrycrush", "junit-jupiter")
    }
}
