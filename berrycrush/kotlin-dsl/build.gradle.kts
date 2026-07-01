plugins {
    id("kotlin-conventions")
    `maven-publish`
    signing
    id("berrycrush.maven-publish")
}

dependencies {
    // Core library dependency
    implementation(project(":berrycrush:core"))
    implementation(project(":berrycrush:junit"))

    // JUnit 5
    implementation(libs.bundles.junit)
    // Testing
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.bundles.mockito)
    testImplementation(libs.jackson.kotlin)
}

// Maven publishing configuration
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name.set("BerryCrush Kotlin DSL")
                description.set("Kotlin DSL for BerryCrush API testing library")
            }
        }
    }
}
