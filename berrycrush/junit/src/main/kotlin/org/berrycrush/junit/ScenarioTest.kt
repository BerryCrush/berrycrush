package org.berrycrush.junit

import org.berrycrush.dsl.BerryCrushSuite
import org.berrycrush.dsl.ScenarioScope
import org.berrycrush.executor.ScenarioExecutor
import org.berrycrush.model.ResultStatus
import org.berrycrush.model.Scenario
import org.berrycrush.model.ScenarioResult
import org.berrycrush.report.ConsoleReporter
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Base class for BerryCrush scenario tests.
 *
 * Extend this class to define and run BDD-style API tests with JUnit 5.
 *
 * Example:
 * ```kotlin
 * @BerryCrushSpec("petstore.yaml")
 * class PetstoreTest : ScenarioTest() {
 *     override fun defineScenarios() {
 *         scenario("List all pets") {
 *             `when`("I request the list of pets") {
 *                 call("listPets")
 *             }
 *             then("I receive a successful response") {
 *                 statusCode(200)
 *                 bodyArrayNotEmpty("$")
 *             }
 *         }
 *     }
 * }
 * ```
 */
@ExtendWith(BerryCrushExtension::class)
abstract class ScenarioTest {
    private val suite: BerryCrushSuite = BerryCrushSuite.create()
    private val reporter = ConsoleReporter()

    /**
     * Override this method to define your scenarios.
     */
    abstract fun defineScenarios()

    /**
     * Override this method to configure the test suite.
     */
    open fun configureSuite() {
        // Default implementation does nothing
    }

    /**
     * Register the OpenAPI spec for this test.
     */
    protected fun spec(
        path: String,
        block: org.berrycrush.config.SpecConfiguration.() -> Unit = {},
    ) {
        suite.spec(path, block)
    }

    /**
     * Register a named OpenAPI spec for multi-spec scenarios.
     */
    protected fun spec(
        name: String,
        path: String,
        block: org.berrycrush.config.SpecConfiguration.() -> Unit = {},
    ) {
        suite.spec(name, path, block)
    }

    /**
     * Configure the test suite.
     */
    protected fun configure(block: org.berrycrush.config.Configuration.() -> Unit) {
        suite.configure(block)
    }

    /**
     * Define a scenario.
     */
    protected fun scenario(
        name: String,
        tags: Set<String> = emptySet(),
        block: ScenarioScope.() -> Unit,
    ): Scenario = suite.scenario(name, tags, block)

    /**
     * Define a scenario outline (parameterized scenario).
     */
    protected fun scenarioOutline(
        name: String,
        tags: Set<String> = emptySet(),
        block: org.berrycrush.dsl.ScenarioOutlineScope.() -> Unit,
    ): List<Scenario> = suite.scenarioOutline(name, tags, block)

    /**
     * Define a reusable fragment.
     */
    protected fun fragment(
        name: String,
        block: org.berrycrush.dsl.FragmentScope.() -> Unit,
    ) {
        suite.fragment(name, block)
    }

    /**
     * JUnit 5 @TestFactory method that generates dynamic tests from scenarios.
     */
    @TestFactory
    fun scenarios(): Collection<DynamicTest> {
        configureSuite()
        defineScenarios()

        val executor = ScenarioExecutor(suite.specRegistry, suite.configuration)

        return suite.allScenarios().map { scenario ->
            DynamicTest.dynamicTest(scenario.name) {
                val result = executor.execute(scenario)
                reporter.onScenarioComplete(result)

                if (result.status == ResultStatus.FAILED) {
                    val failedSteps =
                        result.stepResults
                            .filter { it.status == ResultStatus.FAILED }
                            .joinToString("\n") { step ->
                                "  - ${step.step.description}: ${step.error?.message ?: "Unknown error"}"
                            }
                    throw AssertionError("Scenario '${scenario.name}' failed:\n$failedSteps")
                }

                if (result.status == ResultStatus.SKIPPED) {
                    org.junit.jupiter.api.Assumptions
                        .assumeTrue(false, "Scenario was skipped")
                }
            }
        }
    }

    /**
     * Execute a single scenario and return the result.
     */
    protected fun executeScenario(scenario: Scenario): ScenarioResult {
        val executor = ScenarioExecutor(suite.specRegistry, suite.configuration)
        return executor.execute(scenario)
    }

    /**
     * Get the underlying suite for advanced usage.
     */
    protected fun getSuite(): BerryCrushSuite = suite
}
