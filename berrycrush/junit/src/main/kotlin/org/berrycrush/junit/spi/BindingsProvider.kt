package org.berrycrush.junit.spi

import org.berrycrush.junit.BerryCrushBindings

/**
 * Service Provider Interface for creating BerryCrushBindings instances.
 *
 * Implementations are discovered via Java ServiceLoader.
 * When a test class is annotated with framework-specific annotations
 * (e.g., @BerryCrushContextConfiguration), the corresponding provider
 * handles bindings creation with proper dependency injection.
 *
 * The default binding creation (direct instantiation via reflection) is used
 * when no provider supports the given test class.
 *
 * To implement a custom BindingsProvider:
 * 1. Create a class implementing this interface
 * 2. Register it in META-INF/services/org.berrycrush.junit.spi.BindingsProvider
 *
 * Example for Spring integration:
 * ```kotlin
 * class SpringBindingsProvider : BindingsProvider {
 *     override fun supports(testClass: Class<*>) =
 *         testClass.isAnnotationPresent(BerryCrushContextConfiguration::class.java)
 *
 *     override fun createBindings(testClass: Class<*>, bindingsClass: Class<out BerryCrushBindings>) =
 *         applicationContext.getBean(bindingsClass)
 * }
 * ```
 */
interface BindingsProvider : Provider {
    /**
     * Creates a BerryCrushBindings instance for the given test class.
     *
     * @param testClass The test class being executed
     * @param bindingsClass The bindings class to instantiate (from @BerryCrushConfiguration)
     * @return The bindings instance
     * @throws IllegalStateException if bindings cannot be created
     */
    fun createBindings(
        testClass: Class<*>,
        bindingsClass: Class<out BerryCrushBindings>,
    ): BerryCrushBindings

    /**
     * Creates a test instance for executing @ScenarioTest methods.
     *
     * For providers that manage test lifecycle (e.g., Spring integration),
     * this returns a properly initialized test instance with dependencies injected.
     * The default implementation returns null, causing the executor to create
     * the instance directly via reflection.
     *
     * For Spring integration, this returns a test instance with @LocalServerPort,
     * @Autowired, and other Spring annotations properly injected.
     *
     * @param testClass The test class to create an instance of
     * @return A test instance with dependencies injected, or null to use default instantiation
     */
    fun createTestInstance(testClass: Class<*>): Any? = null
}
