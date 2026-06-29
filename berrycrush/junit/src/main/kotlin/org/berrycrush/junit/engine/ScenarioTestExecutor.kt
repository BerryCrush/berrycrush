package org.berrycrush.junit.engine

import org.berrycrush.assertion.AssertionRegistry
import org.berrycrush.context.ExecutionContext
import org.berrycrush.dsl.BerryCrushSuite
import org.berrycrush.executor.BerryCrushConfigurationProvider
import org.berrycrush.junit.BerryCrushBindings
import org.berrycrush.junit.BerryCrushConfiguration
import org.berrycrush.junit.DefaultBindings
import org.berrycrush.junit.ParallelExecutionMode
import org.berrycrush.junit.binding.OpenApiSpecValue
import org.berrycrush.junit.discovery.FragmentDiscovery
import org.berrycrush.junit.engine.context.TestExecutionContext
import org.berrycrush.junit.spi.BindingsProvider
import org.berrycrush.model.FragmentRegistry
import org.berrycrush.plugin.PluginRegistry
import org.berrycrush.runner.ScenarioRunner
import org.berrycrush.scenario.ScenarioLoader
import org.berrycrush.util.StepRegistry
import org.junit.platform.engine.EngineExecutionListener
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Responsible for executing scenario tests and reporting results.
 *
 * This class handles:
 * - Initializing execution context (bindings, plugins, fragments)
 * - Executing scenario files with proper lifecycle management
 * - Reporting results to JUnit execution listener
 *
 * ## Thread Safety
 *
 * This executor is designed to be thread-safe and supports JUnit 5 parallel execution:
 * - Each scenario gets its own [ExecutionContext] by default
 * - All internal components are stateless or use thread-safe data structures
 * - When `shareVariablesAcrossScenarios=true`, scenarios share context and should run sequentially
 *
 * @see ParallelExecutionMode
 */
class ScenarioTestExecutor(
    private val bindingsProviders: List<BindingsProvider>,
) {
    /**
     * Execute all tests for a class descriptor.
     *
     * Note: Scenario filtering is applied during discovery phase.
     * The filters parameter is accepted for API consistency but filtering
     * has already been applied to the class descriptor's children.
     *
     * @param classDescriptor The test class descriptor containing scenarios to execute
     * @param listener The JUnit execution listener for result reporting
     * @param filters The active scenario filters (applied at discovery, not here)
     */
    @Suppress("UnusedParameter")
    fun executeClassTests(
        classDescriptor: ClassTestDescriptor,
        listener: EngineExecutionListener,
        filters: ScenarioFilters = ScenarioFilters.EMPTY,
    ) {
        val provider = findBindingsProvider(classDescriptor.testClass)

        try {
            provider?.initialize(classDescriptor.testClass.java)
            executeWithContext(classDescriptor, listener, provider)
        } finally {
            runCatching { provider?.cleanup(classDescriptor.testClass.java) }
                .onFailure { logger.severe("Warning: BindingsProvider cleanup failed: ${it.message}") }
        }
    }

    private fun findBindingsProvider(testClass: KClass<*>): BindingsProvider? =
        bindingsProviders.firstOrNull { it.supports(testClass.java) }

    private fun executeWithContext(
        classDescriptor: ClassTestDescriptor,
        listener: EngineExecutionListener,
        provider: BindingsProvider?,
    ) {
        val context = buildExecutionContext(classDescriptor, provider)

        context.beginExecution()
        try {
            context.executeFileDescriptors(classDescriptor, listener)
            context.executeScenarioMethods(classDescriptor, listener, provider)
        } finally {
            context.endExecution()
        }
    }

    // Context building methods

    private fun buildExecutionContext(
        classDescriptor: ClassTestDescriptor,
        provider: BindingsProvider?,
    ): TestExecutionContext {
        val suite = BerryCrushSuite.create()
        val bindings = createBindings(classDescriptor, provider)

        configureSpec(suite, bindings, classDescriptor)

        val pluginRegistry = createPluginRegistry(classDescriptor)
        val fragmentRegistry = loadFragments(classDescriptor)
        val stepRegistry = createStepRegistry(classDescriptor)
        val assertionRegistry = createAssertionRegistry(classDescriptor)
        val configuration = BerryCrushConfigurationProvider.from(suite.configuration)
        val runner =
            ScenarioRunner(
                suite.specRegistry,
                configuration,
                pluginRegistry,
                fragmentRegistry,
                stepRegistry,
                assertionRegistry,
            )

        return TestExecutionContext(
            suite = suite,
            bindings = bindings,
            pluginRegistry = pluginRegistry,
            fragmentRegistry = fragmentRegistry,
            stepRegistry = stepRegistry,
            assertionRegistry = assertionRegistry,
            runner = runner,
        )
    }

    private fun configureSpec(
        suite: BerryCrushSuite,
        bindings: BerryCrushBindings,
        classDescriptor: ClassTestDescriptor,
    ) {
        bindings.configure(suite.configuration)

        val allBindings = bindings.getBindings()

        // Configure OpenAPI specs from bindings (OpenApiSpecValue instances)
        val specsFromBindings = configureSpecsFromBindings(suite, allBindings, classDescriptor)

        // Fall back to @BerryCrushSpec annotations if no specs from bindings
        if (specsFromBindings.isEmpty()) {
            configureSpecsFromAnnotations(suite, classDescriptor)
        }

        // Set global baseUrl if specified as a string (not OpenApiSpecValue)
        allBindings["baseUrl"]?.let { value ->
            if (value !is OpenApiSpecValue) {
                suite.configuration.baseUrl = value.toString()
            }
        }

        // Apply baseUrl from @BerryCrushSpec if set
        classDescriptor.specs["default"]?.baseUrl?.takeIf { it.isNotBlank() }?.let {
            suite.configuration.baseUrl = it
        }
    }

    /**
     * Configure specs from @BerryCrushSpec annotations on the test class.
     */
    private fun configureSpecsFromAnnotations(
        suite: BerryCrushSuite,
        classDescriptor: ClassTestDescriptor,
    ) {
        classDescriptor.specs.forEach { (name, spec) ->
            spec.paths.forEach { path ->
                val resolvedPath = resolvePath(path, classDescriptor.testClass)
                suite.setBaseUrl(name, resolvedPath, spec.baseUrl)
            }
        }
    }

    /**
     * Configure specs from bindings containing OpenApiSpecValue instances.
     * Returns the set of spec names that were configured.
     */
    private fun configureSpecsFromBindings(
        suite: BerryCrushSuite,
        bindings: Map<String, Any>,
        classDescriptor: ClassTestDescriptor,
    ): Set<String> {
        val configuredSpecs = mutableSetOf<String>()
        bindings.forEach { (name, value) ->
            if (value is OpenApiSpecValue) {
                val resolvedPath = resolvePath(value.location, classDescriptor.testClass)
                suite.setBaseUrl(name, resolvedPath, value.baseUrl)
                configuredSpecs.add(name)
            }
        }
        return configuredSpecs
    }

    private fun BerryCrushSuite.setBaseUrl(
        name: String,
        resolvedPath: String,
        value: String?,
    ) {
        if (name == "default") {
            spec(resolvedPath) {
                value.takeIf { it?.isNotBlank() ?: false }?.let { baseUrl = it }
            }
        } else {
            spec(name, resolvedPath) {
                value.takeIf { it?.isNotBlank() ?: false }?.let { baseUrl = it }
            }
        }
    }

    /**
     * Resolve a spec path, supporting both file paths and classpath resources.
     *
     * Paths prefixed with `classpath:` are resolved from the test class's classloader.
     */
    private fun resolvePath(
        path: String,
        testClass: KClass<*>,
    ): String {
        if (!path.startsWith(CLASSPATH_PREFIX)) {
            return path
        }

        val resourcePath = path.removePrefix(CLASSPATH_PREFIX)
        val resource =
            testClass.java.getResource(resourcePath)
                ?: testClass.java.classLoader.getResource(resourcePath.removePrefix("/"))
                ?: throw IllegalArgumentException(
                    "Classpath resource not found: $resourcePath. " +
                        "Make sure the file exists in src/test/resources or src/main/resources.",
                )

        return resource.path
    }

    private fun createBindings(
        classDescriptor: ClassTestDescriptor,
        provider: BindingsProvider?,
    ): BerryCrushBindings {
        val bindingsClass = classDescriptor.bindingsClass ?: DefaultBindings::class.java

        return provider?.let {
            runCatching { it.createBindings(classDescriptor.testClass.java, bindingsClass) }
                .getOrElse { e ->
                    throw IllegalStateException(
                        "BindingsProvider failed to create bindings for class: ${bindingsClass.name}. " +
                            "Cause: ${e.message}",
                        e,
                    )
                }
        } ?: runCatching { bindingsClass.getDeclaredConstructor().newInstance() }.getOrElse { e ->
            throw IllegalStateException(
                "Cannot instantiate bindings class: ${bindingsClass.name}. " +
                    "Ensure it has a public no-arg constructor.",
                e,
            )
        }
    }

    private fun createPluginRegistry(classDescriptor: ClassTestDescriptor): PluginRegistry {
        val registry = PluginRegistry()
        val config =
            classDescriptor.testClass.findAnnotation<BerryCrushConfiguration>()
                ?: return registry

        config.pluginClasses.forEach { pluginClass ->
            runCatching { registry.register(pluginClass) }
                .onFailure {
                    System.err.println("Warning: Failed to register plugin class ${pluginClass.qualifiedName}: ${it.message}")
                }
        }

        config.plugins.forEach { pluginName ->
            runCatching { registry.registerByName(pluginName) }
                .onFailure {
                    System.err.println("Warning: Failed to register plugin '$pluginName': ${it.message}")
                }
        }

        return registry
    }

    private fun loadFragments(classDescriptor: ClassTestDescriptor): FragmentRegistry {
        val registry = FragmentRegistry()
        val fragmentLocations = classDescriptor.fragmentLocations

        if (fragmentLocations.isEmpty()) return registry

        val loader = ScenarioLoader()
        FragmentDiscovery
            .discoverFragments(classDescriptor.testClass.java.classLoader, fragmentLocations)
            .forEach { fragment ->
                runCatching {
                    fragment.url.openStream().use { input ->
                        val content = input.bufferedReader().readText()
                        val fragments = loader.loadFragmentsFromString(content, fragment.name)
                        registry.registerAll(fragments)
                    }
                }.onFailure {
                    System.err.println("Warning: Failed to load fragment from ${fragment.path}: ${it.message}")
                }
            }

        return registry
    }

    /**
     * Creates a StepRegistry with step definitions from @BerryCrushConfiguration.stepClasses.
     * Returns null if no step classes are configured.
     */
    private fun createStepRegistry(classDescriptor: ClassTestDescriptor): StepRegistry? =
        RegistryFactory.createStepRegistry(classDescriptor.testClass.java)

    /**
     * Creates an AssertionRegistry with assertion definitions from @BerryCrushConfiguration.assertionClasses.
     * Returns null if no assertion classes are configured.
     */
    private fun createAssertionRegistry(classDescriptor: ClassTestDescriptor): AssertionRegistry? =
        RegistryFactory.createAssertionRegistry(classDescriptor.testClass.java)

    companion object {
        private const val CLASSPATH_PREFIX = "classpath:"
        private val logger = Logger.getLogger(ScenarioTestExecutor::class.java.name)
    }
}
