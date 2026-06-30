plugins {
    kotlin("jvm")
    id("berrycrush.test-config")
    kotlin("plugin.spring")
}

dependencies {
    // Test against the petstore application
    testImplementation(project(":samples:petstore:app"))

    // BerryCrush core and Kotlin DSL
    testImplementation(project(":berrycrush:core"))
    testImplementation(project(":berrycrush:kotlin-dsl"))
    // BerryCrush JUnit extension
    testImplementation(project(":berrycrush:junit"))

    // BerryCrush Spring integration (for @ScenarioTest with @LocalServerPort)
    testImplementation(project(":berrycrush:spring"))

    // JUnit
    testImplementation(libs.bundles.junit)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.kotlin.test.junit5)

    // Spring Boot test for running the app
    testImplementation(libs.spring.boot.starter.test)
    implementation(kotlin("stdlib"))
}

tasks.test {
    useJUnitPlatform()
}
