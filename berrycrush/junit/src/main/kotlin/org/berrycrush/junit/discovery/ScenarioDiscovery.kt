package org.berrycrush.junit.discovery

import java.net.URL

/**
 * Discovers .scenario files from classpath locations.
 *
 * Supports glob patterns for flexible file matching (e.g., scenarios/`*`.scenario).
 * Delegates discovery logic to [ResourceDiscovery].
 */
object ScenarioDiscovery : ResourceDiscovery<DiscoveredScenario>(
    fileExtension = ".scenario",
    resourceFactory = ::DiscoveredScenario,
) {
    /**
     * Discovers scenario files matching the given location patterns.
     *
     * @param classLoader The class loader to use for resource discovery
     * @param patterns Location patterns (glob syntax supported)
     * @return List of discovered scenario files
     */
    fun discoverScenarios(
        classLoader: ClassLoader,
        patterns: Array<out String>,
    ): List<DiscoveredScenario> = discover(classLoader, patterns)
}

/**
 * Represents a discovered scenario file.
 *
 * @property path The classpath path to the scenario file
 * @property name The filename (without path)
 * @property url The URL to access the scenario file
 */
data class DiscoveredScenario(
    override val path: String,
    override val name: String,
    override val url: URL,
) : Discovered
