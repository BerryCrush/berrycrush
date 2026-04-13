package org.berrycrush.junit.discovery

import java.net.URL

/**
 * Discovers .fragment files from classpath locations.
 *
 * Supports glob patterns for flexible file matching (e.g., fragments/\*.fragment).
 * Delegates discovery logic to [ResourceDiscovery].
 */
object FragmentDiscovery : ResourceDiscovery<DiscoveredFragment>(
    fileExtension = ".fragment",
    resourceFactory = ::DiscoveredFragment,
) {
    /**
     * Discovers fragment files matching the given location patterns.
     *
     * @param classLoader The class loader to use for resource discovery
     * @param patterns Location patterns (glob syntax supported)
     * @return List of discovered fragment files
     */
    fun discoverFragments(
        classLoader: ClassLoader,
        patterns: Array<out String>,
    ): List<DiscoveredFragment> = discover(classLoader, patterns)
}

/**
 * Represents a discovered fragment file.
 *
 * @property path The classpath path to the fragment file
 * @property name The filename (without path)
 * @property url The URL to access the fragment file
 */
data class DiscoveredFragment(
    override val path: String,
    override val name: String,
    override val url: URL,
) : Discovered
