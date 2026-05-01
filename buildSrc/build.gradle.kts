plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
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
    }
}
