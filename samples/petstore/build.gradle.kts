plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Core library dependency
    testImplementation(project(":lemon-check:core"))
    testImplementation(project(":lemon-check:junit"))

    // Testing
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.platform.launcher)
}
