package org.berrycrush.plugin.adapter

import org.berrycrush.context.ExecutionContext
import org.berrycrush.model.Scenario
import org.berrycrush.plugin.ScenarioContext
import java.io.File
import java.nio.file.Path
import java.time.Instant

/**
 * Adapter that bridges [ExecutionContext] with [ScenarioContext] plugin interface.
 *
 * Provides plugin-visible scenario context from existing execution context and scenario model.
 */
class ScenarioContextAdapter(
    private val scenario: Scenario,
    executionContext: ExecutionContext,
    override val startTime: Instant,
    private val sourceFile: File? = null,
    private val scenarioMetadata: Map<String, String> = emptyMap(),
) : ScenarioContext {
    override val scenarioName: String
        get() = scenario.name

    override val scenarioFile: Path
        get() = sourceFile?.toPath() ?: Path.of("unknown")

    // Proxy that delegates to ExecutionContext
    override val variables: MutableMap<String, Any> = ExecutionContextVariablesProxy(executionContext)

    override val metadata: Map<String, String>
        get() = scenarioMetadata

    override val tags: Set<String>
        get() = scenario.tags
}

/**
 * Proxy for ExecutionContext variables that implements MutableMap interface.
 */
private class ExecutionContextVariablesProxy(
    private val context: ExecutionContext,
) : MutableMap<String, Any> {
    private val keySet = mutableSetOf<String>()

    override val entries: MutableSet<MutableMap.MutableEntry<String, Any>>
        get() =
            keySet
                .mapNotNull { key ->
                    context.get<Any>(key)?.let { currentValue ->
                        object : MutableMap.MutableEntry<String, Any> {
                            override val key: String = key
                            override val value: Any
                                get() = context.get<Any>(key) ?: currentValue

                            override fun setValue(newValue: Any): Any {
                                val old = value
                                context[key] = newValue
                                return old
                            }
                        }
                    }
                }.toMutableSet()

    override val keys: MutableSet<String>
        get() = keySet.toMutableSet()

    override val size: Int
        get() = keySet.size

    override val values: MutableCollection<Any>
        get() = keySet.mapNotNull { context.get<Any>(it) }.toMutableList()

    override fun clear() {
        keySet.clear()
    }

    override fun isEmpty(): Boolean = keySet.isEmpty()

    override fun remove(key: String): Any? {
        val value = context.get<Any>(key)
        keySet.remove(key)
        return value
    }

    override fun putAll(from: Map<out String, Any>) {
        for ((key, value) in from) {
            put(key, value)
        }
    }

    override fun put(
        key: String,
        value: Any,
    ): Any? {
        val old = context.get<Any>(key)
        context[key] = value
        keySet.add(key)
        return old
    }

    override fun get(key: String): Any? = context[key]

    override fun containsValue(value: Any): Boolean = values.contains(value)

    override fun containsKey(key: String): Boolean = keySet.contains(key) && context.get<Any>(key) != null
}
