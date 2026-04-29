package org.berrycrush.autotest.provider

import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for auto-test providers using the ServiceLoader pattern.
 *
 * This registry discovers and manages [InvalidTestProvider], [SecurityTestProvider],
 * and [MultiTestProvider] implementations. User-provided implementations with the
 * same `testType` as built-in ones will override the built-in providers (based on priority).
 *
 * ## Usage
 *
 * ```kotlin
 * // Get the default registry (singleton)
 * val registry = AutoTestProviderRegistry.default
 *
 * // Get all invalid test providers
 * val invalidProviders = registry.getInvalidTestProviders()
 *
 * // Get a specific provider
 * val minLengthProvider = registry.getInvalidTestProvider("minLength")
 *
 * // Get all multi-test providers
 * val multiProviders = registry.getMultiTestProviders()
 * ```
 *
 * ## Custom Registry
 *
 * ```kotlin
 * val registry = AutoTestProviderRegistry()
 * registry.registerInvalid(MyCustomProvider())
 * registry.registerSecurity(MySecurityProvider())
 * registry.registerMulti(MyMultiProvider())
 * ```
 *
 * @see InvalidTestProvider
 * @see SecurityTestProvider
 * @see MultiTestProvider
 */
class AutoTestProviderRegistry {
    private val invalidProviders = ConcurrentHashMap<String, InvalidTestProvider>()
    private val securityProviders = ConcurrentHashMap<String, SecurityTestProvider>()
    private val multiProviders = ConcurrentHashMap<String, MultiTestProvider>()

    /**
     * Register an invalid test provider.
     *
     * If a provider with the same testType already exists, the one with
     * higher priority wins. Equal priority means later registration wins.
     */
    fun registerInvalid(provider: InvalidTestProvider) {
        invalidProviders.compute(provider.testType) { _, existing ->
            if (existing == null || provider.priority >= existing.priority) {
                provider
            } else {
                existing
            }
        }
    }

    /**
     * Register a security test provider.
     *
     * If a provider with the same testType already exists, the one with
     * higher priority wins. Equal priority means later registration wins.
     */
    fun registerSecurity(provider: SecurityTestProvider) {
        securityProviders.compute(provider.testType) { _, existing ->
            if (existing == null || provider.priority >= existing.priority) {
                provider
            } else {
                existing
            }
        }
    }

    /**
     * Register a multi-test provider.
     *
     * If a provider with the same testType already exists, the one with
     * higher priority wins. Equal priority means later registration wins.
     */
    fun registerMulti(provider: MultiTestProvider) {
        multiProviders.compute(provider.testType) { _, existing ->
            if (existing == null || provider.priority >= existing.priority) {
                provider
            } else {
                existing
            }
        }
    }

    /**
     * Get all registered invalid test providers.
     */
    fun getInvalidTestProviders(): Collection<InvalidTestProvider> = invalidProviders.values

    /**
     * Get all registered security test providers.
     */
    fun getSecurityTestProviders(): Collection<SecurityTestProvider> = securityProviders.values

    /**
     * Get all registered multi-test providers.
     */
    fun getMultiTestProviders(): Collection<MultiTestProvider> = multiProviders.values

    /**
     * Get an invalid test provider by test type.
     */
    fun getInvalidTestProvider(testType: String): InvalidTestProvider? = invalidProviders[testType]

    /**
     * Get a security test provider by test type.
     */
    fun getSecurityTestProvider(testType: String): SecurityTestProvider? = securityProviders[testType]

    /**
     * Get a multi-test provider by test type.
     */
    fun getMultiTestProvider(testType: String): MultiTestProvider? = multiProviders[testType]

    /**
     * Check if an invalid test type is registered.
     */
    fun hasInvalidTestType(testType: String): Boolean = invalidProviders.containsKey(testType)

    /**
     * Check if a security test type is registered.
     */
    fun hasSecurityTestType(testType: String): Boolean = securityProviders.containsKey(testType)

    /**
     * Check if a multi-test type is registered.
     */
    fun hasMultiTestType(testType: String): Boolean = multiProviders.containsKey(testType)

    /**
     * Get all registered test types (invalid, security, and multi).
     */
    fun getAllTestTypes(): Set<String> = invalidProviders.keys + securityProviders.keys + multiProviders.keys

    companion object {
        /**
         * Default singleton registry with built-in and discovered providers.
         */
        val default: AutoTestProviderRegistry by lazy {
            AutoTestProviderRegistry().apply {
                // Register built-in providers first (priority 0)
                DefaultInvalidTestProviders.all.forEach { registerInvalid(it) }
                DefaultSecurityTestProviders.all.forEach { registerSecurity(it) }
                DefaultMultiTestProviders.all.forEach { registerMulti(it) }

                // Discover user-provided providers via ServiceLoader (higher priority wins)
                ServiceLoader.load(InvalidTestProvider::class.java).forEach { registerInvalid(it) }
                ServiceLoader.load(SecurityTestProvider::class.java).forEach { registerSecurity(it) }
                ServiceLoader.load(MultiTestProvider::class.java).forEach { registerMulti(it) }
            }
        }

        /**
         * Create a new registry with only the default built-in providers.
         */
        fun withDefaults(): AutoTestProviderRegistry =
            AutoTestProviderRegistry().apply {
                DefaultInvalidTestProviders.all.forEach { registerInvalid(it) }
                DefaultSecurityTestProviders.all.forEach { registerSecurity(it) }
                DefaultMultiTestProviders.all.forEach { registerMulti(it) }
            }

        /**
         * Create an empty registry (no built-in providers).
         */
        fun empty(): AutoTestProviderRegistry = AutoTestProviderRegistry()
    }
}
