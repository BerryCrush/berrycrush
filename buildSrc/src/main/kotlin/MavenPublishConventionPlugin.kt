import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
/**
 * Convention plugin for Maven publishing to OSSRH/Sonatype.
 *
 * Apply this plugin to modules that should be published:
 * ```kotlin
 * plugins {
 *     id("berrycrush.maven-publish")
 * }
 * ```
 *
 * The plugin configures:
 * - Maven publication from Java component with sources and javadoc
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
            pluginManager.apply("maven-publish")
            pluginManager.apply("signing")

            afterEvaluate {
                extensions.configure<PublishingExtension> {
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
