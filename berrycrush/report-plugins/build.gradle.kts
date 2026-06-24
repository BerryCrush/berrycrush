plugins {
    id("kotlin-conventions")
    `maven-publish`
    signing
    id("berrycrush.maven-publish")
}

dependencies {
    // These should be implementation
    implementation(project(":berrycrush:api"))
    implementation(project(":berrycrush:plugin"))

    implementation(libs.jackson.kotlin)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.bundles.mockito)
}

// Maven publishing configuration
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name.set("BerryCrush Report plugins")
                description = "Report plugins for OpenAPI-driven BDD-style API testing"
            }
        }
    }
}
