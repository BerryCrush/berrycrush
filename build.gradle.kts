plugins {
    alias(libs.plugins.kotlin.plugin.spring)
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
    //alias(libs.plugins.dokka.core)
    //alias(libs.plugins.dokka.javadoc)
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

subprojects {
    if (path.startsWith(":samples:")) {
        group = "org.berrycrush.samples"
    }
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(file("berrycrush/doc/build/dokka"))
    }
}