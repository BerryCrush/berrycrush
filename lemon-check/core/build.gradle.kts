plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // OpenAPI parsing
    implementation(libs.swagger.parser)

    // JSON processing
    implementation(libs.json.path)
    implementation(libs.json.schema.validator)
    implementation(libs.jackson.kotlin)

    // Testing
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.platform.launcher)
}
