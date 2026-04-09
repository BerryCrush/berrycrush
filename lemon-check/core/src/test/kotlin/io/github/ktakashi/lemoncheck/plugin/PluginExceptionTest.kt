package io.github.ktakashi.lemoncheck.plugin

import io.github.ktakashi.lemoncheck.config.Configuration
import io.github.ktakashi.lemoncheck.executor.ScenarioExecutor
import io.github.ktakashi.lemoncheck.model.Scenario
import io.github.ktakashi.lemoncheck.openapi.SpecRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * Tests for plugin exception handling (fail-fast behavior).
 */
class PluginExceptionTest {
    private lateinit var registry: PluginRegistry

    @BeforeEach
    fun setup() {
        registry = PluginRegistry()
    }

    @Test
    fun `exception in onScenarioStart propagates immediately`() {
        // Given a plugin that throws on scenario start
        val errorPlugin =
            object : LemonCheckPlugin {
                override val name: String = "Error Plugin"
                override val id: String = "error-plugin"

                override fun onScenarioStart(context: ScenarioContext): Unit = throw RuntimeException("Plugin initialization failed")
            }
        registry.register(errorPlugin)

        // When executing a scenario
        val scenario = Scenario(name = "Test")
        val executor = ScenarioExecutor(SpecRegistry(), Configuration(), registry)

        // Then exception should propagate
        val exception =
            assertThrows<RuntimeException> {
                executor.execute(scenario)
            }
        assertEquals("Plugin initialization failed", exception.message)
    }

    @Test
    fun `exception in onScenarioEnd propagates immediately`() {
        val errorPlugin =
            object : LemonCheckPlugin {
                override val name: String = "Error Plugin"
                override val id: String = "error-plugin"

                override fun onScenarioEnd(
                    context: ScenarioContext,
                    result: ScenarioResult,
                ): Unit = throw RuntimeException("Plugin cleanup failed")
            }
        registry.register(errorPlugin)

        val scenario = Scenario(name = "Test")
        val executor = ScenarioExecutor(SpecRegistry(), Configuration(), registry)

        val exception =
            assertThrows<RuntimeException> {
                executor.execute(scenario)
            }
        assertEquals("Plugin cleanup failed", exception.message)
    }

    @Test
    fun `duplicate plugin registration is rejected`() {
        val plugin1 =
            object : LemonCheckPlugin {
                override val id: String = "duplicate-id"
                override val name: String = "Plugin 1"
            }
        val plugin2 =
            object : LemonCheckPlugin {
                override val id: String = "duplicate-id"
                override val name: String = "Plugin 2"
            }

        registry.register(plugin1)

        val exception =
            assertThrows<IllegalArgumentException> {
                registry.register(plugin2)
            }
        assertEquals(exception.message?.contains("duplicate-id"), true)
    }

    @Test
    fun `clear removes all plugins`() {
        registry.register(
            object : LemonCheckPlugin {
                override val id: String = "plugin-1"
            },
        )
        registry.register(
            object : LemonCheckPlugin {
                override val id: String = "plugin-2"
            },
        )

        assertEquals(2, registry.getPlugins().size)

        registry.clear()

        assertEquals(0, registry.getPlugins().size)
    }
}
