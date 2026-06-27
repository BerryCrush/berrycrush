plugins {
    id("kotlin-conventions")
    `maven-publish`
    signing
    id("berrycrush.maven-publish")
}

dependencies {
    api(project(":berrycrush:api"))
}

// Maven publishing configuration
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name.set("BerryCrush Plugin")
                description.set("Custom plugin API library for OpenAPI-driven BDD-style API testing")
            }
        }
    }
}
