import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * Convention plugin for JaCoCo code coverage configuration.
 *
 * This plugin configures JaCoCo for all test tasks with:
 * - HTML and XML report generation
 * - Coverage verification with minimum thresholds
 * - Build fails if coverage drops below thresholds
 *
 * Apply this plugin to modules that need coverage reporting:
 * ```kotlin
 * plugins {
 *     id("berrycrush.jacoco")
 * }
 * ```
 *
 * Generated reports are available at:
 * - HTML: build/reports/jacoco/test/html/index.html
 * - XML: build/reports/jacoco/test/jacocoTestReport.xml
 */
class JacocoConventionPlugin : Plugin<Project> {
    companion object {
        private const val JACOCO_VERSION = "0.8.13"

        private data class CoverageThreshold(
            val line: Double,
            val branch: Double,
        )

        private val DEFAULT_THRESHOLD = CoverageThreshold(line = 0.70, branch = 0.50)
        private val MODULE_THRESHOLDS =
            mapOf(
                "core" to CoverageThreshold(line = 0.65, branch = 0.45),
                "junit" to CoverageThreshold(line = 0.85, branch = 0.80),
                "spring" to CoverageThreshold(line = 0.75, branch = 0.50),
            )
    }

    override fun apply(project: Project) {
        with(project) {
            pluginManager.apply("jacoco")
            val threshold = MODULE_THRESHOLDS[name] ?: DEFAULT_THRESHOLD

            configure<JacocoPluginExtension> {
                toolVersion = JACOCO_VERSION
            }

            // Configure JaCoCo report after tests
            tasks.withType<Test> {
                finalizedBy(tasks.named("jacocoTestReport"))
            }

            // Configure report generation
            tasks.withType<JacocoReport> {
                dependsOn(tasks.withType<Test>())

                reports {
                    xml.required.set(true)
                    html.required.set(true)
                    csv.required.set(false)
                }

                // Exclude generated code, test helpers, and stubs
                classDirectories.setFrom(
                    files(
                        classDirectories.files.map {
                            fileTree(it) {
                                exclude(
                                    "**/generated/**",
                                    "**/*Test*",
                                    "**/*Stub*",
                                    "**/*Mock*",
                                )
                            }
                        },
                    ),
                )
            }

            // Configure coverage verification
            tasks.withType<JacocoCoverageVerification> {
                dependsOn(tasks.withType<JacocoReport>())

                violationRules {
                    rule {
                        limit {
                            counter = "LINE"
                            value = "COVEREDRATIO"
                            minimum = threshold.line.toBigDecimal()
                        }
                    }
                    rule {
                        limit {
                            counter = "BRANCH"
                            value = "COVEREDRATIO"
                            minimum = threshold.branch.toBigDecimal()
                        }
                    }
                }

                // Exclude same classes as report
                classDirectories.setFrom(
                    files(
                        classDirectories.files.map {
                            fileTree(it) {
                                exclude(
                                    "**/generated/**",
                                    "**/*Test*",
                                    "**/*Stub*",
                                    "**/*Mock*",
                                )
                            }
                        },
                    ),
                )
            }

            // Make check depend on coverage verification when check task exists
            tasks.matching { it.name == "check" }.configureEach {
                dependsOn(tasks.withType<JacocoCoverageVerification>())
            }
        }
    }
}
