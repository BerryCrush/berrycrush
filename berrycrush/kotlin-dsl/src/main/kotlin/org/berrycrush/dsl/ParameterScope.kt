package org.berrycrush.dsl

@BerryCrushDsl
class ParameterScope {
    internal val parameters = mutableMapOf<String, Any>()

    operator fun plusAssign(param: Pair<String, Any>) {
        set(param)
    }

    fun set(param: Pair<String, Any>) {
        parameters += param
    }

    fun set(vararg param: Pair<String, Any>) {
        param.forEach { set(it) }
    }
}