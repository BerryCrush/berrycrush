package org.berrycrush.junit.discovery

import org.junit.jupiter.api.Test
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResourceDiscoveryTest {
    private val discovery =
        object : ResourceDiscovery<TestDiscoveredScenario>(
            fileExtension = ".scenario",
            resourceFactory = ::TestDiscoveredScenario,
        ) {}

    @Test
    fun `discover should find scenarios with glob pattern`() {
        val result = discovery.discover(javaClass.classLoader, arrayOf("scenarios/*.scenario"))

        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "simple.scenario" })
        assertTrue(result.any { it.name == "test.scenario" })
    }

    @Test
    fun `discover should support direct path without wildcard`() {
        val result = discovery.discover(javaClass.classLoader, arrayOf("scenarios/simple.scenario"))

        assertEquals(1, result.size)
        assertEquals("simple.scenario", result.single().name)
        assertEquals("scenarios/simple.scenario", result.single().path)
    }

    @Test
    fun `discover should de-duplicate identical matches from multiple patterns`() {
        val result =
            discovery.discover(
                javaClass.classLoader,
                arrayOf("scenarios/*.scenario", "scenarios/simple.scenario"),
            )

        assertEquals(2, result.size)
        assertEquals(2, result.map { it.path }.toSet().size)
    }

    @Test
    fun `discover should return empty for non-matching patterns`() {
        val result = discovery.discover(javaClass.classLoader, arrayOf("scenarios/*.fragment"))

        assertTrue(result.isEmpty())
    }
}

private data class TestDiscoveredScenario(
    override val path: String,
    override val name: String,
    override val url: URL,
) : Discovered
