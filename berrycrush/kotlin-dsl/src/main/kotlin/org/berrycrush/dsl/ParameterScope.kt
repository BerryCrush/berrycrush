package org.berrycrush.dsl

import org.berrycrush.config.BindingConfig

@BerryCrushDsl
class ParameterScope {
    internal val parameters = mutableMapOf<String, Any>()

    operator fun plusAssign(param: Pair<String, Any>) {
        set(param)
    }

    fun binding(block: BindingScope.() -> Unit) = binding(BindingConfig.DEFAULT_BINDING_NAME, block)

    fun binding(
        name: String,
        block: BindingScope.() -> Unit,
    ) {
        val scope = BindingScope(name)
        scope.block()
        set(scope.build())
    }

    fun set(param: Pair<String, Any>) {
        parameters += param
    }

    fun set(vararg param: Pair<String, Any>) {
        param.forEach { set(it) }
    }

    fun set(params: Map<String, Any>) {
        parameters += params
    }
}

@BerryCrushDsl
class BindingScope(
    private val name: String,
) {
    var baseUrl: String? = null

    internal fun build(): Map<String, Any> = mapOf("binding.$name.baseUrl" to baseUrl).filterValues { it != null }.mapValues { it.value!! }
}
