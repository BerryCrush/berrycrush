plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Core library dependency
    implementation(project(":lemon-check:core"))

    // JUnit 5
    implementation(libs.bundles.junit)

    // Testing
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
