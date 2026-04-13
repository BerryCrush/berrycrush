package org.berrycrush.dsl

import org.berrycrush.config.Configuration
import org.berrycrush.config.SpecConfiguration
import org.berrycrush.model.Fragment
import org.berrycrush.model.Scenario
import org.berrycrush.openapi.SpecRegistry

/**
 * Main entry point for BerryCrush test suite definition.
 *
 * Manages OpenAPI spec(s) configuration and scenario definitions.
 */
@BerryCrushDsl
class BerryCrushSuite internal constructor() {
    val specRegistry = SpecRegistry()
    val configuration = Configuration()
    internal val scenarios = mutableListOf<Scenario>()
    internal val fragments = mutableMapOf<String, Fragment>()

    companion object {
        /**
         * Create a new BerryCrush test suite.
         */
        fun create(): BerryCrushSuite = BerryCrushSuite()
    }

    /**
     * Register a single OpenAPI spec (simple API).
     */
    fun spec(
        path: String,
        config: SpecConfiguration.() -> Unit = {},
    ) {
        specRegistry.registerDefault(path, config)
    }

    /**
     * Register a named OpenAPI spec (multi-spec API).
     */
    fun spec(
        name: String,
        path: String,
        config: SpecConfiguration.() -> Unit = {},
    ) {
        specRegistry.register(name, path, config)
    }

    /**
     * Configure the test suite.
     */
    fun configure(block: Configuration.() -> Unit) {
        configuration.apply(block)
    }

    /**
     * Define a scenario.
     */
    fun scenario(
        name: String,
        tags: Set<String> = emptySet(),
        block: ScenarioScope.() -> Unit,
    ): Scenario {
        val scope = ScenarioScope(name, tags, this)
        block(scope)
        val scenario = scope.build()
        scenarios.add(scenario)
        return scenario
    }

    /**
     * Define a scenario outline (parameterized scenario).
     */
    fun scenarioOutline(
        name: String,
        tags: Set<String> = emptySet(),
        block: ScenarioOutlineScope.() -> Unit,
    ): List<Scenario> {
        val scope = ScenarioOutlineScope(name, tags, this)
        block(scope)
        val expandedScenarios = scope.build()
        scenarios.addAll(expandedScenarios)
        return expandedScenarios
    }

    /**
     * Register a fragment for reuse.
     */
    fun fragment(
        name: String,
        block: FragmentScope.() -> Unit,
    ): Fragment {
        val scope = FragmentScope(name)
        block(scope)
        val fragment = scope.build()
        fragments[name] = fragment
        return fragment
    }

    /**
     * Get a registered fragment by name.
     */
    fun getFragment(name: String): Fragment? = fragments[name]

    /**
     * Get all defined scenarios.
     */
    fun allScenarios(): List<Scenario> = scenarios.toList()
}

/**
 * Create a BerryCrush test suite with a single OpenAPI spec.
 */
fun berrycrush(
    openApiSpec: String,
    config: Configuration.() -> Unit = {},
): BerryCrushSuite =
    BerryCrushSuite().apply {
        spec(openApiSpec)
        configure(config)
    }

/**
 * Create a BerryCrush test suite with custom configuration (multi-spec support).
 */
fun berrycrush(config: BerryCrushSuite.() -> Unit): BerryCrushSuite = BerryCrushSuite().apply(config)
