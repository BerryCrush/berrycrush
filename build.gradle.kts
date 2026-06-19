plugins {
    alias(libs.plugins.kotlin.plugin.spring)
    alias(libs.plugins.dokka.core)
    alias(libs.plugins.dokka.javadoc)
    alias(libs.plugins.owasp.dependency.check)
}

// OWASP Dependency Check configuration
dependencyCheck {
    scanConfigurations = listOf("runtimeClasspath", "compileClasspath")
    formats = listOf("HTML", "JSON")
    outputDirectory = layout.buildDirectory.dir("reports/dependency-check")
    nvd {
        apiKey = System.getenv("NVD_API_KEY") ?: ""
    }
}

allprojects {
    group = "org.berrycrush"
    version = version

    repositories {
        mavenCentral()
    }
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(file("berrycrush/doc/build/dokka"))
    }
}