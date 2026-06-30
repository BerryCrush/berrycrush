package org.berrycrush.dsl

import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.config.SpecConfiguration
import org.berrycrush.junit.BerryCrushSuite
import org.berrycrush.model.Fragment
import org.berrycrush.model.Scenario

/**
 * Register a single OpenAPI spec (simple API).
 */
fun BerryCrushSuite.spec(
    path: String,
    config: SpecConfiguration.() -> Unit = {},
) {
    specRegistry.registerDefault(path, config)
}

/**
 * Register a named OpenAPI spec (multi-spec API).
 */
fun BerryCrushSuite.spec(
    name: String,
    path: String,
    config: SpecConfiguration.() -> Unit = {},
) {
    specRegistry.register(name, path, config)
}

/**
 * Configure the test suite.
 */
fun BerryCrushSuite.configure(block: BerryCrushConfiguration.() -> Unit) {
    configuration.apply(block)
}

/**
 * Define a scenario.
 */
fun BerryCrushSuite.scenario(
    name: String,
    tags: Set<String> = emptySet(),
    block: ScenarioScope.() -> Unit,
): Scenario {
    val scope = ScenarioScope(name, tags, this)
    block(scope)
    val scenario = scope.build()
    add(scenario)
    return scenario
}

/**
 * Define a scenario outline (parameterized scenario).
 */
fun BerryCrushSuite.scenarioOutline(
    name: String,
    tags: Set<String> = emptySet(),
    block: ScenarioOutlineScope.() -> Unit,
): List<Scenario> {
    val scope = ScenarioOutlineScope(name, tags, this)
    block(scope)
    val expandedScenarios = scope.build()
    addAll(expandedScenarios)
    return expandedScenarios
}

/**
 * Register a fragment for reuse.
 */
fun BerryCrushSuite.fragment(
    name: String,
    block: FragmentScope.() -> Unit,
): Fragment {
    val scope = FragmentScope(name)
    block(scope)
    val fragment = scope.build()
    add(name, fragment)
    return fragment
}

fun BerryCrushSuite.getFragment(name: String): Fragment? = fragments[name]

/**
 * Create a BerryCrush test suite with a single OpenAPI spec.
 */
fun berrycrush(
    openApiSpec: String,
    config: BerryCrushConfiguration.() -> Unit = {},
): BerryCrushSuite =
    BerryCrushSuite.create().apply {
        spec(openApiSpec)
        configure(config)
    }

/**
 * Create a BerryCrush test suite with custom configuration (multi-spec support).
 */
fun berrycrush(config: BerryCrushSuite.() -> Unit): BerryCrushSuite = BerryCrushSuite.create().apply(config)
