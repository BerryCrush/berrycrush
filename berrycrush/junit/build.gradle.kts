plugins {
    id("kotlin-conventions")
    `maven-publish`
    signing
    id("berrycrush.maven-publish")
}

dependencies {
    // Core library dependency
    implementation(project(":berrycrush:core"))
    implementation(project(":berrycrush:kotlin-dsl"))

    // JUnit 5
    implementation(libs.bundles.junit)

    // JUnit Platform Engine API
    api(libs.bundles.junit.platform)
    api(libs.junit.platform.suite.api)

    // JUnit Platform Launcher API (for TestExecutionListener)
    implementation(libs.junit.platform.launcher)

    // Testing
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.bundles.mockito)
}

// Maven publishing configuration
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name.set("BerryCrush JUnit")
                description.set("JUnit 5 integration for BerryCrush API testing library")
            }
        }
    }
}
