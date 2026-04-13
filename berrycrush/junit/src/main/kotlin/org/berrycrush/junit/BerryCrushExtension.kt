package org.berrycrush.junit

import org.berrycrush.config.Configuration
import org.berrycrush.dsl.BerryCrushSuite
import org.berrycrush.exception.ConfigurationException
import org.berrycrush.executor.ScenarioExecutor
import org.berrycrush.model.Scenario
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import java.util.stream.Stream

/**
 * JUnit 5 extension for BerryCrush scenarios.
 *
 * This extension integrates BerryCrush scenarios with JUnit 5's test framework,
 * enabling scenario execution as JUnit tests with full IDE and CI support.
 *
 * Usage:
 * ```kotlin
 * @ExtendWith(BerryCrushExtension::class)
 * @BerryCrushSpec("api-spec.yaml")
 * class ApiTest : ScenarioTest() {
 *     override fun defineScenarios() {
 *         scenario("List pets") {
 *             `when`("I list all pets") {
 *                 call("listPets")
 *             }
 *             then("I get a list of pets") {
 *                 statusCode(200)
 *             }
 *         }
 *     }
 * }
 * ```
 */
class BerryCrushExtension :
    BeforeAllCallback,
    BeforeEachCallback,
    ParameterResolver,
    TestTemplateInvocationContextProvider {
    companion object {
        private val NAMESPACE = ExtensionContext.Namespace.create(BerryCrushExtension::class.java)
        private const val SUITE_KEY = "berryCrushSuite"
        private const val EXECUTOR_KEY = "scenarioExecutor"
    }

    override fun beforeAll(context: ExtensionContext) {
        val testClass = context.requiredTestClass
        val specAnnotation = testClass.getAnnotation(BerryCrushSpec::class.java)

        val suite = BerryCrushSuite.create()

        // Load spec from annotation
        specAnnotation?.paths?.forEach { path ->
            suite.spec(path)
        }

        // Apply configuration
        specAnnotation?.baseUrl?.takeIf { it.isNotBlank() }?.let {
            suite.configuration.baseUrl = it
        }

        context.getStore(NAMESPACE).put(SUITE_KEY, suite)

        // Create executor
        val executor = ScenarioExecutor(suite.specRegistry, suite.configuration)
        context.getStore(NAMESPACE).put(EXECUTOR_KEY, executor)
    }

    override fun beforeEach(context: ExtensionContext) {
        // Reset execution context for each test
        // Clear any scenario-specific state if needed
    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Boolean {
        val paramType = parameterContext.parameter.type
        return paramType == BerryCrushSuite::class.java ||
            paramType == ScenarioExecutor::class.java ||
            paramType == Configuration::class.java
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Any =
        when (val paramType = parameterContext.parameter.type) {
            BerryCrushSuite::class.java -> getSuite(extensionContext)
            ScenarioExecutor::class.java -> getExecutor(extensionContext)
            Configuration::class.java -> getSuite(extensionContext).configuration
            else -> throw ConfigurationException("Unsupported parameter type: $paramType")
        }

    override fun supportsTestTemplate(context: ExtensionContext): Boolean =
        context.requiredTestMethod.isAnnotationPresent(BerryCrushScenarios::class.java) ||
            context.requiredTestClass.isAnnotationPresent(BerryCrushScenarios::class.java)

    override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> =
        getSuite(context).allScenarios().stream().map { scenario ->
            ScenarioInvocationContext(scenario, getExecutor(context))
        }

    private fun getSuite(context: ExtensionContext): BerryCrushSuite =
        context.getStore(NAMESPACE).get(SUITE_KEY, BerryCrushSuite::class.java)
            ?: throw ConfigurationException("BerryCrushSuite not initialized. Is @ExtendWith(BerryCrushExtension::class) present?")

    private fun getExecutor(context: ExtensionContext): ScenarioExecutor =
        context.getStore(NAMESPACE).get(EXECUTOR_KEY, ScenarioExecutor::class.java)
            ?: throw ConfigurationException("ScenarioExecutor not initialized.")

    /**
     * Context for a single scenario invocation.
     */
    private class ScenarioInvocationContext(
        private val scenario: Scenario,
        private val executor: ScenarioExecutor,
    ) : TestTemplateInvocationContext {
        override fun getDisplayName(invocationIndex: Int): String = scenario.name

        override fun getAdditionalExtensions(): List<org.junit.jupiter.api.extension.Extension> =
            listOf(
                ScenarioParameterResolver(scenario, executor),
            )
    }

    /**
     * Parameter resolver for individual scenarios.
     */
    private class ScenarioParameterResolver(
        private val scenario: Scenario,
        private val executor: ScenarioExecutor,
    ) : ParameterResolver {
        override fun supportsParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext,
        ): Boolean = parameterContext.parameter.type == Scenario::class.java

        override fun resolveParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext,
        ): Any = scenario
    }
}
