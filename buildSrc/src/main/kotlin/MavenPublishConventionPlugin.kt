import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask

/**
 * Convention plugin for Maven publishing to Central Portal.
 *
 * Apply this plugin to modules that should be published. This plugin only configures
 * repositories and signing - it does NOT apply maven-publish or signing plugins.
 * The consuming module must apply these plugins first.
 *
 * For Java/Kotlin modules:
 * ```kotlin
 * plugins {
 *     `maven-publish`
 *     signing
 *     id("berrycrush.maven-publish")
 * }
 * ```
 *
 * For platform (BOM) modules:
 * ```kotlin
 * plugins {
 *     `java-platform`
 *     `maven-publish`
 *     signing
 *     id("berrycrush.maven-publish")
 * }
 * ```
 *
 * The plugin configures:
 * - Central Portal repositories (snapshots and releases)
 * - In-memory GPG signing for CI
 *
 * Required environment variables for publishing:
 * - MAVEN_USERNAME: Sonatype OSSRH username
 * - MAVEN_PASSWORD: Sonatype OSSRH password
 * - SIGNING_KEY: ASCII-armored GPG private key
 * - SIGNING_PASSWORD: GPG key passphrase
 */
class MavenPublishConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            afterEvaluate {
                val sourceSets = project.extensions.getByType<SourceSetContainer>()
                // Create source and javadoc jars
                val sourcesJar by tasks.registering(Jar::class) {
                    description = "source jar generation"
                    archiveClassifier.set("sources")
                    from(sourceSets.named("main").get().allSource)
                }

                val javadocJar by tasks.registering(Jar::class) {
                    description = "javadoc jar generation"
                    archiveClassifier.set("javadoc")
                    val dokkaGeneratePublicationJavadoc = tasks.named("dokkaGeneratePublicationJavadoc")
                    dependsOn(dokkaGeneratePublicationJavadoc)
                    from(dokkaGeneratePublicationJavadoc.flatMap { (it as DokkaGeneratePublicationTask).outputDirectory })
                }

                // Ensure maven-publish is applied
                if (!pluginManager.hasPlugin("maven-publish")) {
                    logger.warn("berrycrush.maven-publish: maven-publish plugin not applied, skipping configuration")
                    return@afterEvaluate
                }
                val publishing = project.extensions.getByType(PublishingExtension::class.java)
                publishing.publications.withType<MavenPublication>().all {
                    if (name != "mavenJava") return@all

                    from(components["java"])
                    artifact(sourcesJar)
                    artifact(javadocJar)
                    pom {
                        url.set("https://github.com/ktakashi/berrycrush")

                        licenses {
                            license {
                                name.set("Apache License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0")
                            }
                        }

                        developers {
                            developer {
                                id.set("ktakashi")
                                name.set("Takashi Kato")
                            }
                        }

                        scm {
                            connection.set("scm:git:git://github.com/ktakashi/berrycrush.git")
                            developerConnection.set("scm:git:ssh://github.com:ktakashi/berrycrush.git")
                            url.set("https://github.com/ktakashi/berrycrush/tree/main")
                        }
                    }

                    repositories {
                        maven {
                            name = "local"
                            url = layout.buildDirectory.dir("repo").get().asFile.toURI()
                        }
                        maven {
                            name = "central"
                            val isSnapshot = version.toString().endsWith("-SNAPSHOT")
                            url = uri(
                                if (isSnapshot) "https://central.sonatype.com/repository/maven-snapshots/"
                                else "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
                            )
                            credentials {
                                username = System.getenv("MAVEN_USERNAME")
                                password = System.getenv("MAVEN_PASSWORD")
                            }
                        }
                    }
                }

                extensions.configure<SigningExtension> {
                    val signingKey = System.getenv("SIGNING_KEY")
                    val signingPassword = System.getenv("SIGNING_PASSWORD")
                    isRequired = signingKey != null && signingPassword != null
                    if (signingKey != null && signingPassword != null) {
                        useInMemoryPgpKeys(signingKey, signingPassword)
                    }
                    val publishing = extensions.getByType<PublishingExtension>()
                    sign(publishing.publications)
                }
            }
        }
    }
}
