package org.berrycrush.plugin

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for plugin priority-based ordering.
 */
class PluginPriorityTest {
    private lateinit var registry: PluginRegistry
    private val executionOrder = mutableListOf<String>()

    @BeforeEach
    fun setup() {
        registry = PluginRegistry()
        executionOrder.clear()
    }

    @Test
    fun `plugins execute in priority order - lower priority first`() {
        // Given plugins with different priorities
        val highPriority = createPlugin("high", 100)
        val mediumPriority = createPlugin("medium", 0)
        val lowPriority = createPlugin("low", -100)

        // Register in arbitrary order
        registry.register(highPriority)
        registry.register(lowPriority)
        registry.register(mediumPriority)

        // When getting plugins
        val plugins = registry.getPlugins()

        // Then they should be sorted by priority (lower first)
        assertEquals(listOf("low", "medium", "high"), plugins.map { it.name })
    }

    @Test
    fun `plugins with same priority maintain registration order`() {
        // Given plugins with same priority registered in specific order
        val plugin1 = createPlugin("first", 0)
        val plugin2 = createPlugin("second", 0)
        val plugin3 = createPlugin("third", 0)

        registry.register(plugin1)
        registry.register(plugin2)
        registry.register(plugin3)

        // When getting plugins
        val plugins = registry.getPlugins()

        // Then they should maintain registration order
        assertEquals(listOf("first", "second", "third"), plugins.map { it.name })
    }

    @Test
    fun `negative priority plugins execute before zero priority`() {
        val setupPlugin = createPlugin("setup", -50)
        val defaultPlugin = createPlugin("default", 0)

        registry.register(defaultPlugin)
        registry.register(setupPlugin)

        val plugins = registry.getPlugins()

        assertEquals("setup", plugins[0].name)
        assertEquals("default", plugins[1].name)
    }

    @Test
    fun `positive priority plugins execute after zero priority`() {
        val cleanupPlugin = createPlugin("cleanup", 50)
        val defaultPlugin = createPlugin("default", 0)

        registry.register(cleanupPlugin)
        registry.register(defaultPlugin)

        val plugins = registry.getPlugins()

        assertEquals("default", plugins[0].name)
        assertEquals("cleanup", plugins[1].name)
    }

    private fun createPlugin(
        pluginName: String,
        pluginPriority: Int,
    ): BerryCrushPlugin =
        object : BerryCrushPlugin {
            override val name: String = pluginName
            override val priority: Int = pluginPriority
            override val id: String = "test-$pluginName-$pluginPriority"
        }
}
