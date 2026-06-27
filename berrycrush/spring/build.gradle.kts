plugins {
    id("kotlin-conventions")
    `maven-publish`
    signing
    id("berrycrush.maven-publish")
    kotlin("plugin.spring")
}

dependencies {
    implementation(kotlin("stdlib"))
    // Core and JUnit module dependencies
    implementation(project(":berrycrush:core"))
    implementation(project(":berrycrush:junit"))

    // Spring Boot Test for TestContextManager
    implementation(libs.spring.boot.starter.test)

    // JUnit Platform for BindingsProvider SPI
    implementation(libs.bundles.junit.platform)

    // Testing
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.bundles.spring.boot)
}

// Maven publishing configuration
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name.set("BerryCrush Spring")
                description.set("Spring Boot integration for BerryCrush API testing library")
            }
        }
    }
}
