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

    companion object {
        const val DEFAULT_BINDING_NAME = BindingConfiguration.DEFAULT_BINDING_NAME
    }
}
