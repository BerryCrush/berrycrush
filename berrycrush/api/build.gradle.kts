plugins {
    id("kotlin-conventions")
    `maven-publish`
    signing
    id("berrycrush.maven-publish")
}

dependencies {
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.bundles.junit)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name.set("BerryCrush Plugin")
                description.set("Scenario API library for OpenAPI-driven BDD-style API testing")
            }
        }
    }
}
