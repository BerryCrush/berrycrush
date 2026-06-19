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

val libsCatalog: VersionCatalog = extensions
    .getByType<VersionCatalogsExtension>()
    .named("libs")

subprojects {
    if (project.displayName.contains(":samples")) return@subprojects

    // Skip BOM module - it uses java-platform which conflicts with java plugins
    if (name == "bom") {
        pluginManager.apply("berrycrush.jacoco")
        return@subprojects
    }
    pluginManager.apply("kotlin-conventions")
    pluginManager.apply("berrycrush.jacoco")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    // Detekt configuration
    configure<dev.detekt.gradle.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
        baseline = file("${projectDir}/config/detekt/baseline.xml")
        parallel = true
        ignoreFailures = false
    }

    // SpotBugs configuration
    configure<com.github.spotbugs.snom.SpotBugsExtension> {
        ignoreFailures.set(false)
        showStackTraces.set(true)
        showProgress.set(true)
        effort.set(com.github.spotbugs.snom.Effort.DEFAULT)
        reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
        excludeFilter.set(file("${rootProject.projectDir}/config/spotbugs/exclusions.xml"))
    }

    tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
        reports.create("html") {
            required.set(true)
        }
        reports.create("xml") {
            required.set(false)
        }
    }

    // CPD configuration
    configure<de.aaschmid.gradle.plugins.cpd.CpdExtension> {
        language = "kotlin"
        minimumTokenCount = 100
        isIgnoreAnnotations = true
        isIgnoreLiterals = true
        isIgnoreIdentifiers = true
        toolVersion = "7.25.0"
    }
    
    tasks.withType<de.aaschmid.gradle.plugins.cpd.Cpd>().configureEach {
        ignoreFailures = false  // Fail build on duplicate code
    }

    // Add Find Security Bugs to SpotBugs
    dependencies {
        add("spotbugsPlugins", libsCatalog.findLibrary("findsecbugs-gradle-plugin").get())
    }

    // SAST aggregate task
    val sast by tasks.registering {
        group = "verification"
        description = "Runs all SAST checks (Detekt, SpotBugs)"
        dependsOn(tasks.named("detekt"))
        tasks.findByName("spotbugsMain")?.let { dependsOn(it) }
    }

    // Full SAST task including CPD
    val sastFull by tasks.registering {
        group = "verification"
        description = "Runs all SAST checks including CPD"
        dependsOn(sast)
        dependsOn(tasks.named("cpdCheck"))
    }

    // Make check task depend on full SAST
    tasks.named("check") {
        dependsOn(sastFull)
    }
}
dokka {
    dokkaPublications.html {
        outputDirectory.set(file("berrycrush/doc/build/dokka"))
    }
}