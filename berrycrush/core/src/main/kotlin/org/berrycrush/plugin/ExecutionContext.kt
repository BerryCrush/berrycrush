package org.berrycrush.plugin

interface ExecutionContext {
    val shareVariablesAcrossScenarios: Boolean

    fun contains(name: String): Boolean

    fun allVariables(): Map<String, Any>

    fun interpolate(template: String): String

    operator fun <T> get(name: String): T?

    operator fun set(
        name: String,
        value: Any,
    )

    fun <T : Any> resolveParam(value: T): T

    fun <T : Any> resolveParams(value: Map<String, T>): Map<String, T>
}
