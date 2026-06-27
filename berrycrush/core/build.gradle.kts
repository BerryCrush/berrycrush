plugins {
    id("kotlin-conventions")
    `maven-publish`
    signing
    id("berrycrush.maven-publish")
}

dependencies {
    // These should be implementation
    api(project(":berrycrush:api"))
    api(project(":berrycrush:plugin"))
    // this needs to be in the classpath
    implementation(project(":berrycrush:report-plugins"))

    // OpenAPI parsing
    implementation(libs.swagger.parser)

    // JSON processing
    implementation(libs.json.path)
    implementation(libs.json.schema.validator)
    implementation(libs.jackson.kotlin)

    // Testing
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.bundles.mockito)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Maven publishing configuration
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name.set("BerryCrush Core")
                description.set("Core library for OpenAPI-driven BDD-style API testing")
            }
        }
    }
}
