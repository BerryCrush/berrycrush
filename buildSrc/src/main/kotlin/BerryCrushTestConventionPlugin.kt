import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

/**
 * Convention plugin for BerryCrush test configuration.
 *
 * This plugin configures all Test tasks to forward system properties
 * with the `berryCrush.` prefix from Gradle to the test JVM.
 *
 * This enables filtering scenarios at runtime:
 * ```bash
 * ./gradlew test -DberryCrush.scenarioFile=login.scenario
 * ./gradlew test -DberryCrush.scenarioName="User Login"
 * ./gradlew test -DberryCrush.featureName="Authentication"
 * ```
 *
 * Apply this plugin to modules that use BerryCrush for testing:
 * ```kotlin
 * plugins {
 *     id("berrycrush.test-config")
 * }
 * ```
 */
class BerryCrushTestConventionPlugin : Plugin<Project> {
    companion object {
        private const val BERRYCRUSH_PROPERTY_PREFIX = "berryCrush."
    }

    override fun apply(project: Project) {
        with(project) {
            tasks.withType<Test>().configureEach {
                // Forward all berryCrush.* system properties to the test JVM
                val berryCrushProperties = System.getProperties()
                    .filter { (key, _) -> key.toString().startsWith(BERRYCRUSH_PROPERTY_PREFIX) }

                berryCrushProperties.forEach { (key, value) ->
                    systemProperty(key.toString(), value.toString())
                }

                // Also check Gradle project properties
                project.properties
                    .filter { (key, _) -> key.startsWith(BERRYCRUSH_PROPERTY_PREFIX) }
                    .forEach { (key, value) ->
                        if (value != null) {
                            systemProperty(key, value.toString())
                        }
                    }
            }
        }
    }
}
