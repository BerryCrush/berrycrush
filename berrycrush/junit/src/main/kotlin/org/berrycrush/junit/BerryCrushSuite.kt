package org.berrycrush.junit

import org.berrycrush.executor.BerryCrushConfigurationProvider
import org.berrycrush.executor.BerryCrushScenarioExecutor
import org.berrycrush.model.Fragment
import org.berrycrush.model.Scenario
import org.berrycrush.openapi.SpecRegistry
import org.berrycrush.config.BerryCrushConfiguration as Configuration

interface BerryCrushSuite {
    companion object {
        fun create(): BerryCrushSuite = DefaultBerryCrushSuite()
    }

    val configuration: Configuration
    val scenarios: List<Scenario>
    val specRegistry: SpecRegistry
    val fragments: Map<String, Fragment>

    fun add(scenario: Scenario)

    fun addAll(scenarios: List<Scenario>)

    fun add(
        name: String,
        fragment: Fragment,
    )

    fun register(path: String) = specRegistry.registerDefault(path)

    fun register(
        name: String,
        path: String,
    ) = specRegistry.register(name, path)

    fun toScenarioExecutor() = BerryCrushScenarioExecutor(specRegistry, BerryCrushConfigurationProvider.from(configuration))
}

private data class DefaultBerryCrushSuite(
    override val configuration: Configuration = Configuration(),
    override val scenarios: MutableList<Scenario> = mutableListOf(),
    override val specRegistry: SpecRegistry = SpecRegistry(),
    override val fragments: MutableMap<String, Fragment> = mutableMapOf(),
) : BerryCrushSuite {
    override fun add(scenario: Scenario) {
        scenarios.add(scenario)
    }

    override fun addAll(scenarios: List<Scenario>) {
        this.scenarios.addAll(scenarios)
    }

    override fun add(
        name: String,
        fragment: Fragment,
    ) {
        fragments[name] = fragment
    }
}
