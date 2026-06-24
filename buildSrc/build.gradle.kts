plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.spotbugs.gradle.plugin)
    implementation(libs.findsecbugs.gradle.plugin)
    implementation(libs.cpd.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("maven-publish-convention") {
            id = "berrycrush.maven-publish"
            implementationClass = "MavenPublishConventionPlugin"
        }
        register("test-config-convention") {
            id = "berrycrush.test-config"
            implementationClass = "BerryCrushTestConventionPlugin"
        }
        register("jacoco-convention") {
            id = "berrycrush.jacoco"
            implementationClass = "JacocoConventionPlugin"
        }
        // internal purpose, see kotlin-convensions.gradle.kts
        register("build-time-dependency-convention") {
            id = "berrycrush.dependency"
            implementationClass = "BuildTimeDependencyPlugin"
        }
    }
}
