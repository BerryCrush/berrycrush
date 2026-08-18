package org.berrycrush.junit.discovery

import java.net.URL

object SchemaDiscovery : ResourceDiscovery<DiscoveredSchema>(
    fileExtension = ".[yaml,json]",
    resourceFactory = ::DiscoveredSchema,
) {
    // exact match
    override fun buildGlobPattern(pattern: String): String = pattern

    // accept both yaml and json files
    override fun resourceFilter(pattern: String): Boolean = pattern.endsWith(".yaml") || pattern.endsWith(".json")
}

data class DiscoveredSchema(
    override val path: String,
    override val name: String,
    override val url: URL,
) : Discovered
