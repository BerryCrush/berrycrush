package org.berrycrush.junit.spi

interface Provider {
    /**
     * Determines if this provider supports the given test class.
     *
     * @param testClass The test class being executed
     * @return true if this provider can handle bindings for this test class
     */
    fun supports(testClass: Class<*>): Boolean

    /**
     * Priority of this provider. Higher values indicate higher priority.
     * When multiple providers support a test class, the one with highest
     * priority is used.
     *
     * @return Priority value (default implementations should return 0)
     */
    fun priority(): Int = 0

    /**
     * Initializes the provider for the given test class.
     * Called once before any scenarios are executed.
     *
     * For Spring integration, this starts the ApplicationContext.
     *
     * @param testClass The test class being executed
     */
    fun initialize(testClass: Class<*>)

    /**
     * Cleans up resources after test execution completes.
     * Called once after all scenarios have executed.
     *
     * For Spring integration, this releases the ApplicationContext.
     *
     * @param testClass The test class that was executed
     */
    fun cleanup(testClass: Class<*>)
}
