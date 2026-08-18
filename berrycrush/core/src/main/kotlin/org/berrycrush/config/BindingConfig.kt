package org.berrycrush.config

import org.berrycrush.configuration.BindingConfiguration
import org.berrycrush.openapi.LoadedSpec
import org.berrycrush.openapi.LoadedSpecProvider
import org.berrycrush.openapi.SpecRegistry

data class BindingConfig(
    override val name: String,
    override val baseUrl: String? = null,
    override val location: String? = null,
    override val operationAliases: Map<String, String> = emptyMap(),
) : BindingConfiguration,
    LoadedSpecProvider {
    override val spec: LoadedSpec? by lazy {
        location?.let {
            SpecRegistry.load(name, it) {
                this.baseUrl = baseUrl
            }
        }
    }

    fun toBuilder() = Builder(name, baseUrl, location, operationAliases.toMutableMap())

    companion object {
        const val DEFAULT_BINDING_NAME = BindingConfiguration.DEFAULT_BINDING_NAME

        @JvmStatic @JvmOverloads
        fun builder(name: String = DEFAULT_BINDING_NAME) = Builder(name, null, null, mutableMapOf())
    }

    class Builder internal constructor(
        private var name: String,
        private var baseUrl: String?,
        private var location: String?,
        private var operationAliases: MutableMap<String, String>,
    ) {
        fun name(name: String) = apply { this.name = name }

        fun baseUrl(url: String) = apply { this.baseUrl = url }

        fun location(location: String) = apply { this.location = location }

        fun operationAlias(
            alias: String,
            api: String,
        ) = apply { this.operationAliases[alias] = api }

        fun operationAliases(operationAliases: Map<String, String>) = apply { this.operationAliases = operationAliases.toMutableMap() }

        fun build(): BindingConfig =
            BindingConfig(
                name = name,
                baseUrl = baseUrl,
                location = location,
                operationAliases = operationAliases,
            )
    }
}
