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
    }
}
